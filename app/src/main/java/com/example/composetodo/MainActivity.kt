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
import androidx.core.content.ContextCompat
import com.example.composetodo.navigation.ToDoNavigation
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.ui.theme.ComposeToDoTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val viewModel: TaskPresenter by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Permiso de notificaciones concedido")
        } else {
            Log.w(TAG, "Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "Iniciando onCreate")
            
            processNotificationIntent()
            requestNotificationPermission()
            setupUserInterface()
            
            Log.d(TAG, "MainActivity onCreate completado con éxito")
        } catch (e: Exception) {
            handleCriticalError(e)
        }
    }
    
    private fun processNotificationIntent() {
        val taskId = intent.getIntExtra("taskId", -1)
        if (taskId != -1) {
            Log.d(TAG, "Se abrió la app desde una notificación con taskId: $taskId")
            viewModel.getTaskById(taskId)
        }
    }
    
    private fun setupUserInterface() {
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
    }
    
    private fun handleCriticalError(e: Exception) {
        Log.e(TAG, "Error en onCreate", e)
        try {
            setContent {
                ComposeToDoTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // UI de error (vacía por ahora)
                    }
                }
            }
        } catch (e2: Exception) {
            Log.e(TAG, "Error crítico al inicializar la UI alternativa", e2)
        }
    }
    
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
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        Log.d(TAG, "Se debería mostrar explicación para solicitar permiso")
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        Log.d(TAG, "Solicitando permiso de notificaciones")
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