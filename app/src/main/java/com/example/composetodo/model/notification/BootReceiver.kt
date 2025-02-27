package com.example.composetodo.model.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.presenter.NotificationPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.widget.Toast
import kotlinx.coroutines.delay

/**
 * BroadcastReceiver que se ejecuta después de que el dispositivo se reinicie
 * para reprogramar todas las notificaciones pendientes
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver: onReceive llamado con acción: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Dispositivo reiniciado, reprogramando notificaciones")
            
            // Mostrar un Toast para informar al usuario
            Toast.makeText(
                context,
                "Reprogramando recordatorios de tareas...",
                Toast.LENGTH_LONG
            ).show()
            
            // Usar un scope para las corrutinas
            val scope = CoroutineScope(Dispatchers.IO)
            
            scope.launch {
                try {
                    // Esperar un poco para asegurarnos de que el sistema esté completamente iniciado
                    delay(5000)
                    
                    Log.d(TAG, "Obteniendo tareas desde la base de datos")
                    val taskDao = TaskDatabase.getDatabase(context).taskDao()
                    val notificationPresenter = NotificationPresenter(context)
                    
                    // Obtener todas las tareas con recordatorios que aún no han pasado
                    val now = LocalDateTime.now()
                    
                    // Usamos "first()" con import explícito para recolectar el Flow
                    val tasks = try {
                        taskDao.getAllTasks().first()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al obtener las tareas", e)
                        emptyList()
                    }
                    
                    Log.d(TAG, "Se encontraron ${tasks.size} tareas en total")
                    
                    // Filtramos las tareas que tienen recordatorios pendientes
                    val pendingTasks = tasks.filter { task -> 
                        !task.isCompleted && 
                        task.reminderDateTime != null && 
                        task.reminderDateTime.isAfter(now)
                    }
                    
                    Log.d(TAG, "Se encontraron ${pendingTasks.size} tareas con recordatorio pendiente")
                    
                    // Reprogramar todas las notificaciones
                    for (task in pendingTasks) {
                        Log.d(TAG, "Reprogramando notificación para tarea ID=${task.id}, título=${task.title}, recordatorio=${task.reminderDateTime?.format(DateTimeFormatter.ISO_DATE_TIME)}")
                        notificationPresenter.scheduleNotification(task)
                        
                        // Pequeña pausa para no sobrecargar el sistema
                        delay(100)
                    }
                    
                    Log.d(TAG, "Reprogramación de notificaciones completada")
                    
                    // Mostrar un Toast para confirmar
                    if (pendingTasks.isNotEmpty()) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                "Se han reprogramado ${pendingTasks.size} recordatorios",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en el proceso de reprogramación de notificaciones", e)
                    
                    // Mostrar un Toast para informar del error
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Error al reprogramar recordatorios: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            Log.d(TAG, "Acción desconocida: ${intent.action}")
        }
    }
} 