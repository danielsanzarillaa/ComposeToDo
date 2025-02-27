package com.example.composetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.composetodo.navigation.ToDoNavigation
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.ui.theme.ComposeToDoTheme
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val viewModel: TaskPresenter by viewModels()

    // Registrar el lanzador para solicitar permisos de notificación
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Permiso de notificaciones concedido")
            // El permiso ha sido concedido, podemos mostrar notificaciones
            // No es necesario hacer nada más aquí, las notificaciones funcionarán automáticamente
        } else {
            Log.w(TAG, "Permiso de notificaciones denegado")
            // El permiso ha sido denegado, podríamos mostrar un mensaje al usuario
            // explicando por qué las notificaciones son importantes
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "Iniciando onCreate")
            super.onCreate(savedInstanceState)
            
            // Verificar si se recibió un taskId del intent de la notificación
            val taskId = intent.getIntExtra("taskId", -1)
            if (taskId != -1) {
                Log.d(TAG, "Se abrió la app desde una notificación con taskId: $taskId")
                // Aquí puedes cargar la tarea específica si lo necesitas
                viewModel.getTaskById(taskId)
            }
            
            // Solicitar permiso de notificaciones en Android 13+
            requestNotificationPermission()
            
            Log.d(TAG, "Configurando la interfaz de usuario")
            setContent {
                ComposeToDoTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ToDoNavigation(viewModel)
                    }
                }
            }
            Log.d(TAG, "MainActivity onCreate completado con éxito")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate", e)
            // En caso de error, intentar mostrar al menos algo básico
            try {
                setContent {
                    ComposeToDoTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // Mostrar una UI de error si es posible
                        }
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Error crítico al inicializar la UI alternativa", e2)
            }
        }
    }
    
    /**
     * Solicita el permiso de notificación en Android 13+ si no está concedido
     */
    private fun requestNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "Verificando permisos de notificación (Android 13+)")
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d(TAG, "Permiso de notificaciones ya concedido")
                        // El permiso ya está concedido
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        Log.d(TAG, "Se debería mostrar explicación para solicitar permiso")
                        // Se podría mostrar un diálogo explicativo sobre por qué necesitamos el permiso
                        // Después de mostrar el diálogo, solicitar el permiso
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        Log.d(TAG, "Solicitando permiso de notificaciones")
                        // Solicitar el permiso directamente
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                Log.d(TAG, "No es necesario solicitar permisos en Android pre-13")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al solicitar permisos de notificación", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume llamado")
        
        // Verificar si hay tareas con recordatorios próximos (en los próximos 5 minutos)
        checkUpcomingReminders()
    }
    
    /**
     * Verifica si hay tareas con recordatorios próximos
     */
    private fun checkUpcomingReminders() {
        try {
            Log.d(TAG, "Verificando recordatorios próximos")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = LocalDateTime.now()
                    // Obtener hasta 5 minutos en el futuro
                    val fiveMinutesLater = now.plusMinutes(5)
                    
                    // Usando el método suspendido
                    val upcomingTasks = viewModel.getTasksWithRemindersInRange(now, fiveMinutesLater)
                    
                    Log.d(TAG, "Se encontraron ${upcomingTasks.size} tareas con recordatorios en los próximos 5 minutos")
                    
                    // Si hay tareas próximas, mostrar alguna indicación en la UI
                    if (upcomingTasks.isNotEmpty()) {
                        viewModel.notifyUpcomingReminders(upcomingTasks)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al verificar recordatorios próximos", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar verificación de recordatorios", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause llamado")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy llamado")
    }
}