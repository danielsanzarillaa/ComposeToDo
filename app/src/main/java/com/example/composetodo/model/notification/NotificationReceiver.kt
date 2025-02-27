package com.example.composetodo.model.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.ActivityManager
import android.os.Build
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import android.os.PowerManager
import java.time.format.DateTimeFormatter
import com.example.composetodo.model.Priority

/**
 * BroadcastReceiver que recibe las intenciones de alarma y muestra las notificaciones
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Verificar que la acción sea la correcta
        if (intent.action != ACTION_SHOW_NOTIFICATION) {
            return
        }
        
        val taskId = intent.getIntExtra("taskId", -1)
        val isBackup = intent.getBooleanExtra("isBackup", false)
        val isPreview = intent.getBooleanExtra("isPreview", false)
        val isTest = taskId == 999999 // ID especial para pruebas
        
        // Obtener datos adicionales que pueden venir en el intent
        val taskTitle = intent.getStringExtra("taskTitle")
        val taskDescription = intent.getStringExtra("taskDescription")
        val scheduledTime = intent.getStringExtra("scheduledTime")
        
        // Si es una notificación de respaldo, verificar si la principal ya se mostró
        if (isBackup && !isTest) {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val notificationKey = "notification_shown_$taskId"
            
            if (sharedPrefs.getBoolean(notificationKey, false)) {
                return
            }
        }
        
        if (taskId != -1) {
            // Adquirir wakelock para asegurar que la operación se complete
            val wakeLock = acquireWakeLock(context)
            
            try {
                // Para tareas de prueba, mostrar la notificación inmediatamente
                if (isTest) {
                    val testTask = Task(
                        id = taskId,
                        title = "Tarea de prueba",
                        description = "Esta es una notificación de prueba generada a las ${LocalDateTime.now()}",
                        priority = Priority.ALTA
                    )
                    
                    // Mostrar la notificación en el hilo principal
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val notificationBuilder = NotificationBuilder(context)
                            notificationBuilder.showTaskNotification(testTask)
                        } catch (e: Exception) {
                            // Manejo silencioso de errores
                        } finally {
                            // Liberar el wakelock
                            releaseWakeLock(wakeLock)
                        }
                    }
                    return
                }
                
                // Si tenemos los datos completos en el intent, podemos mostrar la notificación sin consultar la base de datos
                if (taskTitle != null && taskDescription != null) {
                    // Crear una tarea temporal con los datos del intent
                    val tempTask = Task(
                        id = taskId,
                        title = taskTitle,
                        description = taskDescription ?: "",
                        priority = Priority.ALTA, // Prioridad por defecto
                        reminderDateTime = if (scheduledTime != null) {
                            try {
                                LocalDateTime.parse(scheduledTime, DateTimeFormatter.ISO_DATE_TIME)
                            } catch (e: Exception) {
                                LocalDateTime.now()
                            }
                        } else {
                            LocalDateTime.now()
                        }
                    )
                    
                    // Mostrar la notificación directamente
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val notificationBuilder = NotificationBuilder(context)
                            val hasPermission = notificationBuilder.hasNotificationPermission()
                            
                            if (hasPermission) {
                                // Personalizar el mensaje según el tipo de notificación
                                val finalTask = when {
                                    isPreview -> {
                                        // Para notificaciones previas, añadir un prefijo al título
                                        tempTask.copy(
                                            title = "⏰ Recordatorio previo: ${tempTask.title}",
                                            description = "Esta tarea está programada para dentro de 5 minutos. ${tempTask.description}"
                                        )
                                    }
                                    isBackup -> {
                                        // Para notificaciones de respaldo, añadir un sufijo al título
                                        tempTask.copy(
                                            title = "${tempTask.title} (recordatorio)",
                                            description = "¡No olvides esta tarea! ${tempTask.description}"
                                        )
                                    }
                                    else -> tempTask
                                }
                                
                                notificationBuilder.showTaskNotification(finalTask)
                                
                                // Marcar la notificación como mostrada
                                if (!isPreview) {
                                    markNotificationAsShown(context, taskId)
                                }
                            }
                        } catch (e: Exception) {
                            // Manejo silencioso de errores
                        } finally {
                            releaseWakeLock(wakeLock)
                        }
                    }
                    return
                }
                
                // Si no tenemos los datos completos, consultamos la base de datos
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val task = TaskDatabase.getDatabase(context).taskDao().getTaskById(taskId)
                        
                        task?.let {
                            // Para notificaciones de respaldo, verificamos si la tarea sigue pendiente
                            if (isBackup) {
                                // Procesar notificación de respaldo
                            } else if (isPreview) {
                                // Procesar notificación previa
                            }
                            
                            // Solo mostrar notificación si la tarea aún existe, no está completada,
                            // y el recordatorio no ha caducado hace mucho
                            if (!it.isCompleted && isReminderValid(it)) {
                                // Comprobamos si la aplicación está en primer plano
                                val isAppInForeground = isAppInForeground(context)
                                
                                // Siempre mostrar la notificación, incluso si la app está en primer plano
                                withContext(Dispatchers.Main) {
                                    try {
                                        val notificationBuilder = NotificationBuilder(context)
                                        val hasPermission = notificationBuilder.hasNotificationPermission()
                                        
                                        if (hasPermission) {
                                            // Personalizar el mensaje según el tipo de notificación
                                            val finalTask = when {
                                                isPreview -> {
                                                    // Para notificaciones previas, añadir un prefijo al título
                                                    it.copy(
                                                        title = "⏰ Recordatorio previo: ${it.title}",
                                                        description = "Esta tarea está programada para dentro de 5 minutos. ${it.description}"
                                                    )
                                                }
                                                isBackup -> {
                                                    // Para notificaciones de respaldo, añadir un sufijo al título
                                                    it.copy(
                                                        title = "${it.title} (recordatorio)",
                                                        description = "¡No olvides esta tarea! ${it.description}"
                                                    )
                                                }
                                                else -> it
                                            }
                                            
                                            notificationBuilder.showTaskNotification(finalTask)
                                            
                                            // Marcar la notificación como mostrada
                                            if (!isPreview) {
                                                markNotificationAsShown(context, taskId)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Manejo silencioso de errores
                                    }
                                }
                            } else {
                                // No mostrar notificación por alguna razón específica
                                // (tarea completada o recordatorio no válido)
                            }
                        } ?: run {
                            // Si no encontramos la tarea en la base de datos pero tenemos datos en el intent,
                            // intentamos mostrar la notificación con esos datos
                            if (taskTitle != null) {
                                withContext(Dispatchers.Main) {
                                    try {
                                        val tempTask = Task(
                                            id = taskId,
                                            title = taskTitle,
                                            description = taskDescription ?: "",
                                            priority = Priority.ALTA
                                        )
                                        
                                        val notificationBuilder = NotificationBuilder(context)
                                        if (notificationBuilder.hasNotificationPermission()) {
                                            notificationBuilder.showTaskNotification(tempTask)
                                            
                                            // Marcar la notificación como mostrada
                                            if (!isPreview) {
                                                markNotificationAsShown(context, taskId)
                                            }
                                        } else {
                                            // No hay permisos, no hacer nada
                                        }
                                    } catch (e: Exception) {
                                        // Manejo silencioso de errores
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Manejo silencioso de errores
                    } finally {
                        // Liberar el wakelock
                        releaseWakeLock(wakeLock)
                    }
                }
            } catch (e: Exception) {
                releaseWakeLock(wakeLock)
            }
        }
    }
    
    /**
     * Marca una notificación como mostrada para evitar duplicados
     */
    private fun markNotificationAsShown(context: Context, taskId: Int) {
        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val notificationKey = "notification_shown_$taskId"
            
            sharedPrefs.edit().apply {
                putBoolean(notificationKey, true)
                apply()
            }
        } catch (e: Exception) {
            // Manejo silencioso de errores
        }
    }
    
    /**
     * Verifica si un recordatorio sigue siendo válido
     * (no ha pasado más de 5 minutos desde su hora programada)
     */
    private fun isReminderValid(task: Task): Boolean {
        val reminderDateTime = task.reminderDateTime ?: return false
        val now = LocalDateTime.now()
        
        // Si el recordatorio es futuro, es válido
        if (reminderDateTime.isAfter(now)) {
            return true
        }
        
        // Si el recordatorio es pasado pero no ha pasado más de 5 minutos, aún es válido
        val fiveMinutesAgo = now.minusMinutes(5)
        return reminderDateTime.isAfter(fiveMinutesAgo)
    }
    
    /**
     * Adquiere un wakelock para asegurar que el procesamiento se complete
     */
    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ComposeToDo:NotificationWakeLock"
            )
            
            // Adquirir el wakelock con timeout de 60 segundos
            wakeLock.acquire(60000L)
            
            return wakeLock
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Libera el wakelock
     */
    private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        if (wakeLock == null) return
        
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            // Manejo silencioso de errores
        }
    }
    
    /**
     * Comprueba si la aplicación está actualmente en primer plano
     */
    private fun isAppInForeground(context: Context): Boolean {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Diferentes implementaciones según la versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                val runningProcesses = activityManager.runningAppProcesses
                if (runningProcesses != null) {
                    for (processInfo in runningProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                            processInfo.processName == context.packageName) {
                            return true
                        }
                    }
                }
                return false
            } else {
                // Versiones anteriores
                val appProcesses = activityManager.runningAppProcesses
                if (appProcesses != null) {
                    val packageName = context.packageName
                    for (appProcess in appProcesses) {
                        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && 
                            appProcess.processName == packageName) {
                            return true
                        }
                    }
                }
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_SHOW_NOTIFICATION = "com.example.composetodo.SHOW_NOTIFICATION"
    }
} 