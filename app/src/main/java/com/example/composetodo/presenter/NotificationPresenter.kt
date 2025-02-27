package com.example.composetodo.presenter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.composetodo.model.Task
import com.example.composetodo.model.notification.NotificationBuilder
import com.example.composetodo.model.notification.NotificationReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

/**
 * Presentador responsable de manejar la lógica de programación de notificaciones
 */
class NotificationPresenter(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationPresenter"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val notificationBuilder = NotificationBuilder(context)
    private val alarmManager: AlarmManager? by lazy {
        try {
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo AlarmManager", e)
            null
        }
    }
    
    // Coroutine scope para operaciones asíncronas
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Programa una notificación para una tarea con reintentos en caso de fallo
     */
    fun scheduleNotification(task: Task) {
        scope.launch {
            var success = false
            var attempts = 0
            
            while (!success && attempts < MAX_RETRY_ATTEMPTS) {
                attempts++
                Log.d(TAG, "Intento $attempts de programar notificación para tarea ID=${task.id}")
                
                success = try {
                    scheduleNotificationInternal(task)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error en intento $attempts al programar notificación para tarea ID=${task.id}", e)
                    delay(RETRY_DELAY_MS) // Esperamos antes de reintentar
                    false
                }
            }
            
            if (!success) {
                Log.e(TAG, "No se pudo programar la notificación después de $MAX_RETRY_ATTEMPTS intentos para tarea ID=${task.id}")
                // Como último recurso, intentamos mostrar la notificación inmediatamente si está cerca
                if (isReminderSoon(task.reminderDateTime)) {
                    withContext(Dispatchers.Main) {
                        notificationBuilder.showTaskNotification(task)
                    }
                }
            }
        }
    }
    
    /**
     * Determina si un recordatorio es próximo (menos de 5 minutos)
     */
    private fun isReminderSoon(reminderDateTime: LocalDateTime?): Boolean {
        if (reminderDateTime == null) return false
        
        val now = LocalDateTime.now()
        val fiveMinutesLater = now.plusMinutes(5)
        
        return reminderDateTime.isAfter(now) && reminderDateTime.isBefore(fiveMinutesLater)
    }
    
    /**
     * Implementación interna de la programación de notificaciones
     */
    private suspend fun scheduleNotificationInternal(task: Task): Boolean {
        // Solo programar si la tarea tiene un recordatorio y no está completada
        if (task.reminderDateTime == null) {
            Log.d(TAG, "No se programa notificación para la tarea ID=${task.id} porque no tiene recordatorio")
            return false
        }
        
        if (task.isCompleted) {
            Log.d(TAG, "No se programa notificación para la tarea ID=${task.id} porque está completada")
            return false
        }
        
        val reminderTimeMillis = task.reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        
        // No programar si el tiempo ya pasó
        if (reminderTimeMillis <= now) {
            Log.d(TAG, "No se programa notificación para la tarea ID=${task.id} porque la fecha ya pasó: ${task.reminderDateTime.format(DateTimeFormatter.ISO_DATE_TIME)}")
            return false
        }
        
        Log.d(TAG, "Programando notificación para tarea ID=${task.id} a las ${task.reminderDateTime.format(DateTimeFormatter.ISO_DATE_TIME)}")
        
        // Crear intent para el BroadcastReceiver
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_SHOW_NOTIFICATION
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("taskDescription", task.description)
            putExtra("scheduledTime", task.reminderDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
        }
        
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            flags
        )
        
        // Verificar si tenemos AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "No se puede programar la notificación porque AlarmManager es null")
            return false
        }
        
        // Configurar la alarma según la versión de Android
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == true -> {
                    Log.d(TAG, "Programando alarma exacta con setExactAndAllowWhileIdle (Android 12+)")
                    alarmManager?.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                    
                    // Verificar que la alarma se haya programado correctamente
                    val isAlarmSet = PendingIntent.getBroadcast(
                        context, task.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    ) != null
                    
                    Log.d(TAG, "¿Alarma programada correctamente? $isAlarmSet")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    Log.d(TAG, "Programando alarma exacta con setExactAndAllowWhileIdle (Android 6+)")
                    alarmManager?.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                    
                    // Verificar que la alarma se haya programado correctamente
                    val isAlarmSet = PendingIntent.getBroadcast(
                        context, task.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    ) != null
                    
                    Log.d(TAG, "¿Alarma programada correctamente? $isAlarmSet")
                }
                else -> {
                    Log.d(TAG, "Programando alarma exacta con setExact (Android pre-6)")
                    alarmManager?.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                    
                    // Verificar que la alarma se haya programado correctamente
                    val isAlarmSet = PendingIntent.getBroadcast(
                        context, task.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    ) != null
                    
                    Log.d(TAG, "¿Alarma programada correctamente? $isAlarmSet")
                }
            }
            
            // Ya no programamos alarmas de respaldo
            // Tampoco programamos alarmas previas
            
            Log.d(TAG, "Notificación programada correctamente para tarea ID=${task.id}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar la alarma para tarea ID=${task.id}", e)
            return false
        }
    }
    
    /**
     * Cancela una notificación programada para una tarea
     */
    fun cancelNotification(taskId: Int) {
        try {
            Log.d(TAG, "Cancelando notificación para tarea ID=$taskId")
            
            // Cancelar la notificación principal
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_SHOW_NOTIFICATION
                putExtra("taskId", taskId)
            }
            
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                flags
            )
            
            // Verificar si tenemos AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "No se puede cancelar la notificación porque AlarmManager es null")
                return
            }
            
            // Cancelar la alarma principal
            alarmManager?.cancel(pendingIntent)
            
            // Cancelar el PendingIntent
            try {
                pendingIntent.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar PendingIntent", e)
            }
            
            // Limpiar las preferencias compartidas
            try {
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val notificationKey = "notification_shown_$taskId"
                sharedPrefs.edit().remove(notificationKey).apply()
                Log.d(TAG, "Preferencias de notificación limpiadas para tarea ID=$taskId")
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar preferencias de notificación", e)
            }
            
            Log.d(TAG, "Notificación cancelada correctamente para tarea ID=$taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar la notificación para tarea ID=$taskId", e)
        }
    }
    
    /**
     * Reprograma una notificación para una tarea
     */
    fun rescheduleNotification(task: Task) {
        try {
            Log.d(TAG, "Reprogramando notificación para tarea ID=${task.id}")
            cancelNotification(task.id)
            scheduleNotification(task)
        } catch (e: Exception) {
            Log.e(TAG, "Error al reprogramar la notificación para tarea ID=${task.id}", e)
        }
    }
    
    /**
     * Comprueba si la aplicación tiene permiso para mostrar notificaciones
     */
    fun hasNotificationPermission(): Boolean {
        return notificationBuilder.hasNotificationPermission()
    }
    
    /**
     * Cancela todas las operaciones en curso cuando se destruye el ViewModel
     */
    fun onCleared() {
        scope.cancel()
    }
} 