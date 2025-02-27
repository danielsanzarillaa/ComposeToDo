package com.example.composetodo.model.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * BroadcastReceiver que recibe las intenciones de alarma y muestra las notificaciones
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_SHOW_NOTIFICATION = "com.example.composetodo.SHOW_NOTIFICATION"
        private const val MAX_REMINDER_VALIDITY_MINUTES = 5
        private const val WAKELOCK_TIMEOUT_MS = 60000L // 60 segundos
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_NOTIFICATION) return
        
        val taskId = intent.getIntExtra("taskId", -1)
        if (taskId == -1) return
        
        val isBackup = intent.getBooleanExtra("isBackup", false)
        
        if (isBackup && isBackupAlreadyShown(context, taskId)) return
        
        val taskTitle = intent.getStringExtra("taskTitle")
        val taskDescription = intent.getStringExtra("taskDescription") ?: ""
        val scheduledTime = intent.getStringExtra("scheduledTime")
        
        Log.d(TAG, "Procesando notificación para tarea: $taskId, título: $taskTitle")
        
        val wakeLock = acquireWakeLock(context)
        processNotificationWithWakeLock(context, taskId, taskTitle, taskDescription, scheduledTime, isBackup, wakeLock)
    }
    
    private fun processNotificationWithWakeLock(
        context: Context,
        taskId: Int,
        taskTitle: String?,
        taskDescription: String,
        scheduledTime: String?,
        isBackup: Boolean,
        wakeLock: PowerManager.WakeLock?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processNotification(
                    context, taskId, taskTitle, taskDescription,
                    scheduledTime, isBackup
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar notificación: ${e.message}", e)
            } finally {
                releaseWakeLock(wakeLock)
            }
        }
    }
    
    private suspend fun processNotification(
        context: Context,
        taskId: Int,
        taskTitle: String?,
        taskDescription: String,
        scheduledTime: String?,
        isBackup: Boolean
    ) {
        try {
            val dbTask = TaskDatabase.getDatabase(context).taskDao().getTaskById(taskId)
            Log.d(TAG, if (dbTask != null) 
                        "Tarea encontrada en DB: ${dbTask.title}" 
                      else 
                        "Tarea no encontrada en DB, usando datos del intent")
            
            val taskToShow = determineTaskToShow(
                dbTask, taskId, taskTitle, taskDescription, 
                scheduledTime, isBackup
            )
            
            showNotificationIfPossible(context, taskToShow, taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el procesamiento de la notificación", e)
        }
    }
    
    private fun determineTaskToShow(
        dbTask: Task?,
        taskId: Int,
        taskTitle: String?,
        taskDescription: String,
        scheduledTime: String?,
        isBackup: Boolean
    ): Task? = when {
        dbTask != null && !dbTask.isCompleted && isReminderValid(dbTask) -> {
            customizeTaskMessage(dbTask, isBackup)
        }
        taskTitle != null -> {
            createTaskFromIntent(taskId, taskTitle, taskDescription, scheduledTime, isBackup)
        }
        else -> null
    }
    
    private fun customizeTaskMessage(task: Task, isBackup: Boolean): Task = when {
        isBackup -> task.copy(
            title = "${task.title} (recordatorio)",
            description = "¡No olvides esta tarea! ${task.description}"
        )
        else -> task
    }
    
    private suspend fun showNotificationIfPossible(
        context: Context, 
        taskToShow: Task?, 
        taskId: Int
    ) {
        if (taskToShow == null) {
            Log.w(TAG, "No hay información suficiente para mostrar la notificación")
            return
        }
        
        Log.d(TAG, "Mostrando notificación para: ${taskToShow.title}")
        withContext(Dispatchers.Main) {
            val notificationBuilder = NotificationBuilder(context)
            if (notificationBuilder.hasNotificationPermission()) {
                notificationBuilder.showTaskNotification(taskToShow)
                markNotificationAsShown(context, taskId)
            } else {
                Log.w(TAG, "No hay permiso para mostrar notificaciones")
            }
        }
    }
    
    private fun createTaskFromIntent(
        taskId: Int,
        taskTitle: String,
        taskDescription: String,
        scheduledTime: String?,
        isBackup: Boolean
    ): Task {
        val baseTask = Task(
            id = taskId,
            title = taskTitle,
            description = taskDescription,
            priority = Priority.ALTA,
            reminderDateTime = parseScheduledTime(scheduledTime)
        )
        
        return customizeTaskMessage(baseTask, isBackup)
    }
    
    private fun parseScheduledTime(scheduledTime: String?): LocalDateTime = 
        scheduledTime?.let {
            try {
                LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                Log.w(TAG, "Error al parsear fecha: $it, usando actual", e)
                LocalDateTime.now()
            }
        } ?: LocalDateTime.now()
    
    private fun isBackupAlreadyShown(context: Context, taskId: Int): Boolean = try {
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            .getBoolean("notification_shown_$taskId", false)
    } catch (e: Exception) {
        Log.e(TAG, "Error al verificar estado de notificación: ${e.message}", e)
        false
    }
    
    private fun markNotificationAsShown(context: Context, taskId: Int) {
        try {
            context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("notification_shown_$taskId", true)
                .apply()
            
            Log.d(TAG, "Notificación marcada como mostrada: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al marcar notificación como mostrada: ${e.message}", e)
        }
    }
    
    private fun isReminderValid(task: Task): Boolean {
        val reminderDateTime = task.reminderDateTime ?: return false
        val now = LocalDateTime.now()
        
        if (reminderDateTime.isAfter(now)) return true
        
        val cutoffTime = now.minusMinutes(MAX_REMINDER_VALIDITY_MINUTES.toLong())
        return reminderDateTime.isAfter(cutoffTime)
    }
    
    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? = try {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.run {
            newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ComposeToDo:NotificationWakeLock"
            ).apply {
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al adquirir wakelock: ${e.message}", e)
        null
    }
    
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