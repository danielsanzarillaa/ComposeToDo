package com.example.composetodo.model.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.composetodo.model.Task
import com.example.composetodo.MainActivity
import android.graphics.Color
import android.media.RingtoneManager

/**
 * Clase responsable de construir y mostrar notificaciones
 * siguiendo el patrón MVP como parte del modelo
 */
class NotificationBuilder(private val context: Context) {

    companion object {
        private const val TAG = "NotificationBuilder"
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Recordatorios de Tareas"
        const val CHANNEL_DESCRIPTION = "Notificaciones para recordatorios de tareas pendientes"
    }

    init {
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            // Manejo silencioso de errores
        }
    }

    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Comprueba si la aplicación tiene permiso para mostrar notificaciones
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores a Android 13, no se necesita permiso explícito
        }
    }

    /**
     * Construye y muestra una notificación para una tarea
     */
    fun showTaskNotification(task: Task) {
        try {
            if (!hasNotificationPermission()) {
                return
            }
            
            // Intent para abrir la aplicación cuando se toque la notificación
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("taskId", task.id)
            }
            
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                context, 
                task.id, 
                intent,
                flags
            )

            // Obtener sonido de notificación predeterminado
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Construir la notificación
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Icono de alarma estándar
                .setContentTitle(task.title)
                .setContentText(if (task.description.isNotEmpty()) task.description else "Tarea pendiente")
                .setPriority(NotificationCompat.PRIORITY_MAX) // Prioridad máxima
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                // Asegurarnos de que se muestre incluso en primer plano
                .setOnlyAlertOnce(false)
                // Comportamiento para servicios en primer plano (Android 11+)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría de alarma para mayor prioridad
                // Hacer que la notificación vibre y haga sonido
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Sonido personalizado
                .setSound(defaultSoundUri)
                // Vibración personalizada
                .setVibrate(longArrayOf(0, 500, 200, 500))
                // Luz LED
                .setLights(Color.RED, 1000, 500)
                // Mostrar la hora de la notificación
                .setShowWhen(true)
                // Asegurar que se muestra en primer plano en Android 11+
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Hacer que la notificación sea persistente
                .setOngoing(false)
                // Estilo de notificación expandible
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(task.description.ifEmpty { "Tarea pendiente" })
                    .setBigContentTitle(task.title)
                    .setSummaryText("Recordatorio de tarea"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            }

            // Mostrar la notificación
            try {
                val notificationManager = NotificationManagerCompat.from(context)
                
                if (hasNotificationPermission()) {
                    notificationManager.notify(task.id, builder.build())
                }
            } catch (e: Exception) {
                // Intento alternativo usando NotificationManager directamente
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(task.id, builder.build())
                } catch (e2: Exception) {
                    // Manejo silencioso de errores
                }
            }
        } catch (e: Exception) {
            // Manejo silencioso de errores
        }
    }
} 