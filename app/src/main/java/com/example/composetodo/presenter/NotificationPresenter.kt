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
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Presentador responsable de programar notificaciones para tareas
 */
class NotificationPresenter(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationPresenter"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val notificationBuilder = NotificationBuilder(context)
    private val alarmManager: AlarmManager? by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Programa una notificación para una tarea con reintentos
     */
    fun scheduleNotification(task: Task) {
        scope.launch {
            var success = false
            var attempts = 0
            
            while (!success && attempts < MAX_RETRY_ATTEMPTS) {
                attempts++
                
                success = try {
                    scheduleNotificationInternal(task)
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Error al programar notificación (intento $attempts): ${e.message}")
                    delay(RETRY_DELAY_MS)
                    false
                }
            }
            
            if (!success) {
                Log.w(TAG, "No se pudo programar la notificación después de $MAX_RETRY_ATTEMPTS intentos")
            }
        }
    }
    
    /**
     * Implementación interna de la programación de alarmas
     */
    private fun scheduleNotificationInternal(task: Task): Boolean {
        val reminderDateTime = task.reminderDateTime ?: return false
        if (task.isCompleted) return false
        
        val reminderTimeMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        
        if (reminderTimeMillis <= now) return false
        
        val intent = createNotificationIntent(task, reminderDateTime)
        val pendingIntent = createPendingIntent(task.id, intent)
        
        return alarmManager?.let {
            scheduleExactAlarm(it, reminderTimeMillis, pendingIntent)
            true
        } ?: run {
            Log.e(TAG, "AlarmManager no disponible")
            false
        }
    }
    
    private fun scheduleExactAlarm(alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
            )
        }
    }
    
    /**
     * Crea un intent para la notificación
     */
    private fun createNotificationIntent(task: Task, reminderDateTime: LocalDateTime): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_SHOW_NOTIFICATION
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("taskDescription", task.description)
            putExtra("scheduledTime", reminderDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
        }
    }
    
    /**
     * Crea un PendingIntent para la notificación
     */
    private fun createPendingIntent(taskId: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            flags
        )
    }
    
    /**
     * Comprueba si se pueden programar alarmas exactas (compatible con API < 31)
     */
    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean = 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                Log.w(TAG, "Error al verificar permisos de alarmas exactas: ${e.message}")
                false
            }
        } else {
            true
        }
    
    /**
     * Cancela una notificación programada
     */
    fun cancelNotification(taskId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_SHOW_NOTIFICATION
            putExtra("taskId", taskId)
        }
        
        val pendingIntent = createPendingIntent(taskId, intent)
        
        alarmManager?.cancel(pendingIntent)
        
        try {
            pendingIntent.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error al cancelar pendingIntent: ${e.message}")
        }
        
        clearNotificationPreferences(taskId)
    }
    
    private fun clearNotificationPreferences(taskId: Int) {
        try {
            context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("notification_shown_$taskId")
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error al limpiar preferencias: ${e.message}")
        }
    }
} 