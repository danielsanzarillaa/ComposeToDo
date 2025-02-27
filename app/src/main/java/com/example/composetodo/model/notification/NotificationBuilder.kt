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
import android.util.Log
import com.example.composetodo.model.Task
import com.example.composetodo.MainActivity
import android.graphics.Color
import android.media.RingtoneManager
import android.widget.Toast

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
            Log.e(TAG, "Error al crear el canal de notificaciones", e)
            Toast.makeText(context, "Error al crear canal de notificaciones: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creando canal de notificaciones")
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
            Log.d(TAG, "Canal de notificaciones creado correctamente")
            
            // Verificar que el canal se creó correctamente
            val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (createdChannel != null) {
                Log.d(TAG, "Canal verificado: ${createdChannel.name}, importancia: ${createdChannel.importance}")
            } else {
                Log.e(TAG, "¡Error! El canal no se creó correctamente")
                Toast.makeText(context, "Error: No se pudo crear el canal de notificaciones", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Comprueba si la aplicación tiene permiso para mostrar notificaciones
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Permisos de notificación (Android 13+): $hasPermission")
            
            if (!hasPermission) {
                Toast.makeText(context, "¡No tienes permisos de notificación! Ve a Configuración > Apps > ComposeToDo > Notificaciones", Toast.LENGTH_LONG).show()
            }
            
            hasPermission
        } else {
            Log.d(TAG, "Permisos de notificación (Android pre-13): true")
            true // En versiones anteriores a Android 13, no se necesita permiso explícito
        }
    }

    /**
     * Construye y muestra una notificación para una tarea
     */
    fun showTaskNotification(task: Task) {
        try {
            if (!hasNotificationPermission()) {
                Log.e(TAG, "No se tienen permisos para mostrar notificaciones")
                return
            }

            Log.d(TAG, "Preparando notificación para tarea: ID=${task.id}, título=${task.title}")
            
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
                    Log.d(TAG, "Notificación mostrada correctamente para tarea ID=${task.id}")
                    
                    // Mostrar un Toast para confirmar que la notificación se envió
                    Toast.makeText(context, "Notificación enviada: ${task.title}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "No se pudo mostrar la notificación por falta de permisos")
                    Toast.makeText(context, "Error: No tienes permisos para mostrar notificaciones", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al mostrar la notificación con NotificationManagerCompat", e)
                
                // Intento alternativo usando NotificationManager directamente
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(task.id, builder.build())
                    Log.d(TAG, "Notificación mostrada correctamente usando NotificationManager directo")
                    Toast.makeText(context, "Notificación enviada (método alternativo): ${task.title}", Toast.LENGTH_SHORT).show()
                } catch (e2: Exception) {
                    Log.e(TAG, "Error crítico al mostrar la notificación incluso con método alternativo", e2)
                    Toast.makeText(context, "Error crítico al mostrar notificación: ${e2.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error general al mostrar la notificación", e)
            Toast.makeText(context, "Error general al mostrar notificación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
} 