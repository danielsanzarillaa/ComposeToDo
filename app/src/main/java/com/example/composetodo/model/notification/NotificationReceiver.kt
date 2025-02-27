package com.example.composetodo.model.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import android.os.PowerManager
import java.time.format.DateTimeFormatter
import com.example.composetodo.model.Priority
import android.util.Log

/**
 * BroadcastReceiver que recibe las intenciones de alarma y muestra las notificaciones
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_SHOW_NOTIFICATION = "com.example.composetodo.SHOW_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Verificar que la acción sea la correcta
        if (intent.action != ACTION_SHOW_NOTIFICATION) return
        
        val taskId = intent.getIntExtra("taskId", -1)
        if (taskId == -1) return
        
        val isBackup = intent.getBooleanExtra("isBackup", false)
        val isPreview = intent.getBooleanExtra("isPreview", false)
        
        // Si es una notificación de respaldo, verificar si la principal ya se mostró
        if (isBackup && isBackupAlreadyShown(context, taskId)) return
        
        // Obtener datos adicionales que pueden venir en el intent
        val taskTitle = intent.getStringExtra("taskTitle")
        val taskDescription = intent.getStringExtra("taskDescription") ?: ""
        val scheduledTime = intent.getStringExtra("scheduledTime")
        
        Log.d(TAG, "Procesando notificación para tarea: $taskId, título: $taskTitle")
        
        // Adquirir wakelock para asegurar que la operación se complete
        val wakeLock = acquireWakeLock(context)
        if (wakeLock == null) {
            Log.w(TAG, "No se pudo adquirir wakelock para la tarea: $taskId")
        }
        
        // Estructura principal protegida con try-finally para garantizar la liberación del wakeLock
        try {
            // Utilizamos coroutines para todo el procesamiento
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processNotification(
                        context = context,
                        taskId = taskId,
                        taskTitle = taskTitle,
                        taskDescription = taskDescription,
                        scheduledTime = scheduledTime,
                        isPreview = isPreview,
                        isBackup = isBackup
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar notificación: ${e.message}", e)
                } finally {
                    // Liberar el wakelock, siempre se ejecuta
                    releaseWakeLock(wakeLock)
                }
            }
        } catch (e: Exception) {
            // En caso de error crítico, aseguramos la liberación del wakeLock
            Log.e(TAG, "Error crítico al iniciar procesamiento: ${e.message}", e)
            releaseWakeLock(wakeLock)
        }
    }
    
    /**
     * Procesa una notificación normal (no de prueba)
     */
    private suspend fun processNotification(
        context: Context,
        taskId: Int,
        taskTitle: String?,
        taskDescription: String,
        scheduledTime: String?,
        isPreview: Boolean,
        isBackup: Boolean
    ) {
        try {
            // Intentamos obtener la tarea de la base de datos
            val dbTask = TaskDatabase.getDatabase(context).taskDao().getTaskById(taskId)
            
            if (dbTask != null) {
                Log.d(TAG, "Tarea encontrada en DB: ${dbTask.title}")
            } else {
                Log.d(TAG, "Tarea no encontrada en DB, usando datos del intent")
            }
            
            // Definimos la tarea a mostrar
            val taskToShow = when {
                // Caso 1: Tenemos la tarea en la base de datos y está vigente
                dbTask != null && !dbTask.isCompleted && isReminderValid(dbTask) -> {
                    // Personalizar el mensaje según el tipo de notificación
                    when {
                        isPreview -> dbTask.copy(
                            title = "⏰ Recordatorio previo: ${dbTask.title}",
                            description = "Esta tarea está programada para dentro de 5 minutos. ${dbTask.description}"
                        )
                        isBackup -> dbTask.copy(
                            title = "${dbTask.title} (recordatorio)",
                            description = "¡No olvides esta tarea! ${dbTask.description}"
                        )
                        else -> dbTask
                    }
                }
                // Caso 2: No tenemos la tarea en la DB pero tenemos al menos el título
                taskTitle != null -> {
                    createTaskFromIntent(taskId, taskTitle, taskDescription, scheduledTime, isPreview, isBackup)
                }
                // Caso 3: No tenemos información suficiente para mostrar la notificación
                else -> null
            }
            
            // Si tenemos una tarea para mostrar, lo hacemos
            if (taskToShow != null) {
                Log.d(TAG, "Mostrando notificación para: ${taskToShow.title}")
                withContext(Dispatchers.Main) {
                    val notificationBuilder = NotificationBuilder(context)
                    if (notificationBuilder.hasNotificationPermission()) {
                        notificationBuilder.showTaskNotification(taskToShow)
                        
                        // Para notificaciones normales, marcarlas como mostradas
                        // Para vistas previas, solo registrar en el log
                        if (!isPreview) {
                            markNotificationAsShown(context, taskId)
                        } else {
                            Log.d(TAG, "Vista previa de notificación mostrada, no se marca como mostrada permanentemente")
                        }
                    } else {
                        Log.w(TAG, "No hay permiso para mostrar notificaciones")
                    }
                }
            } else {
                Log.w(TAG, "No hay información suficiente para mostrar la notificación")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el procesamiento de la notificación: ${e.message}", e)
        }
    }
    
    /**
     * Crea una tarea a partir de los datos del intent
     */
    private fun createTaskFromIntent(
        taskId: Int,
        taskTitle: String,
        taskDescription: String,
        scheduledTime: String?,
        isPreview: Boolean,
        isBackup: Boolean
    ): Task {
        val baseTask = Task(
            id = taskId,
            title = taskTitle,
            description = taskDescription,
            priority = Priority.ALTA,
            reminderDateTime = parseScheduledTime(scheduledTime)
        )
        
        return when {
            isPreview -> baseTask.copy(
                title = "⏰ Recordatorio previo: ${baseTask.title}",
                description = "Esta tarea está programada para dentro de 5 minutos. ${baseTask.description}"
            )
            isBackup -> baseTask.copy(
                title = "${baseTask.title} (recordatorio)",
                description = "¡No olvides esta tarea! ${baseTask.description}"
            )
            else -> baseTask
        }
    }
    
    /**
     * Convierte un string de fecha en LocalDateTime
     */
    private fun parseScheduledTime(scheduledTime: String?): LocalDateTime {
        if (scheduledTime == null) return LocalDateTime.now()
        
        return try {
            LocalDateTime.parse(scheduledTime, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            Log.w(TAG, "Error al parsear fecha: $scheduledTime, usando actual", e)
            LocalDateTime.now()
        }
    }
    
    /**
     * Verifica si una notificación de respaldo ya se mostró
     */
    private fun isBackupAlreadyShown(context: Context, taskId: Int): Boolean {
        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val notificationKey = "notification_shown_$taskId"
            return sharedPrefs.getBoolean(notificationKey, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado de notificación: ${e.message}", e)
            return false
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
            Log.d(TAG, "Notificación marcada como mostrada: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al marcar notificación como mostrada: ${e.message}", e)
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
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ComposeToDo:NotificationWakeLock"
            ).apply {
                acquire(60000L) // Adquirir con timeout de 60 segundos
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al adquirir wakelock: ${e.message}", e)
            null
        }
    }
    
    /**
     * Libera el wakelock de forma segura
     */
    private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
                Log.d(TAG, "Wakelock liberado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar wakelock: ${e.message}", e)
        }
    }
} 