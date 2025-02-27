package com.example.composetodo.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composetodo.model.Task
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.components.calendarComponents.Calendar
import com.example.composetodo.view.components.calendarComponents.CalendarTopBar
import com.example.composetodo.view.components.calendarComponents.TaskByDateCalendarComponent
import java.time.LocalDate
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke

/**
 * Pantalla de calendario que muestra un calendario y las tareas para la fecha seleccionada.
 * Permite navegar entre fechas, ver, añadir, editar y eliminar tareas.
 *
 * Esta pantalla sigue el patrón MVP:
 * - View: Esta pantalla y sus componentes visuales
 * - Presenter: TaskPresenter que maneja la lógica de negocio
 * - Model: Task y otras entidades de datos
 *
 * @param viewModel Presentador que maneja la lógica de negocio de las tareas
 * @param onNavigateBack Función para navegar hacia atrás
 * @param onNavigateToAddTask Función para navegar a la pantalla de añadir tarea
 * @param onNavigateToEditTask Función para navegar a la pantalla de editar tarea
 */
@Composable
fun CalendarScreen(
    viewModel: TaskPresenter,
    onNavigateBack: () -> Unit,
    onNavigateToAddTask: (LocalDate) -> Unit,
    onNavigateToEditTask: (Int) -> Unit
) {
    // Estado
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasksForDate by viewModel.tasksForSelectedDate.collectAsState(initial = emptyList())
    val today = LocalDate.now()
    var lastDeletedTask by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTestNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CalendarTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Componente de calendario
                Calendar(
                    selectedDate = selectedDate,
                    onDateSelected = { date -> viewModel.setSelectedDate(date) },
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Contenedor de tareas para la fecha seleccionada
                TaskByDateCalendarComponent(
                    selectedDate = selectedDate,
                    tasksForDate = tasksForDate,
                    today = today,
                    viewModel = viewModel,
                    onNavigateToAddTask = onNavigateToAddTask,
                    onNavigateToEditTask = onNavigateToEditTask,
                    snackbarHostState = snackbarHostState,
                    scope = scope,
                    onTaskDeleted = { task -> lastDeletedTask = task },
                    lastDeletedTask = lastDeletedTask,
                    onUndoDelete = { lastDeletedTask = null },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Botón de prueba para notificaciones
            Button(
                onClick = { showTestNotificationDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.9f)
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3D00) // Naranja intenso para mayor visibilidad
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                border = BorderStroke(2.dp, Color.Yellow)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Probar notificación",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "PROBAR NOTIFICACIÓN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        "Pulsa para verificar si funcionan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
            // Diálogo para confirmar la notificación de prueba
            if (showTestNotificationDialog) {
                AlertDialog(
                    onDismissRequest = { showTestNotificationDialog = false },
                    title = { Text("Probar Notificación") },
                    text = { 
                        Column {
                            Text("¿Quieres enviar una notificación de prueba inmediatamente?")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Esto mostrará una notificación de prueba para verificar si el sistema de notificaciones funciona correctamente.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            val hasPermission = viewModel.hasNotificationPermission()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Estado de permisos: " + if (hasPermission) "✅ CONCEDIDOS" else "❌ DENEGADOS",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasPermission) Color.Green else Color.Red
                            )
                            
                            if (!hasPermission) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Debes conceder permisos de notificación en la configuración de la aplicación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Red
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    val testTask = Task(
                                        id = 999999, // ID especial para prueba
                                        title = "Tarea de prueba",
                                        description = "Esta es una notificación de prueba generada a las ${LocalDateTime.now()}",
                                        priority = com.example.composetodo.model.Priority.ALTA,
                                        reminderDateTime = LocalDateTime.now().plusSeconds(1)
                                    )
                                    viewModel.testNotification(testTask)
                                    snackbarHostState.showSnackbar(
                                        message = "Notificación de prueba enviada"
                                    )
                                }
                                showTestNotificationDialog = false
                            }
                        ) {
                            Text("Probar Ahora")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTestNotificationDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
} 