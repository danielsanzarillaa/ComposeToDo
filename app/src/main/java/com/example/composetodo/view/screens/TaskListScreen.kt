package com.example.composetodo.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composetodo.model.Task
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.components.taskListComponents.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import java.time.LocalDateTime
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke

/**
 * Pantalla principal que muestra la lista de tareas agrupadas por fecha.
 * Permite navegar a otras pantallas, añadir, editar y eliminar tareas.
 *
 * Esta pantalla sigue el patrón MVP:
 * - View: Esta pantalla y sus componentes visuales
 * - Presenter: TaskPresenter que maneja la lógica de negocio
 * - Model: Task y otras entidades de datos
 *
 * @param viewModel Presentador que maneja la lógica de negocio de las tareas
 * @param onNavigateToAddTask Función para navegar a la pantalla de añadir tarea
 * @param onNavigateToCalendar Función para navegar a la pantalla de calendario
 * @param onNavigateToEditTask Función para navegar a la pantalla de editar tarea
 */
@Composable
fun TaskListScreen(
    viewModel: TaskPresenter,
    onNavigateToAddTask: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToEditTask: (Int) -> Unit
) {
    val tasksGroupedByDate by viewModel.allTasksGroupedByDate.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = LocalDate.now()
    var showTestNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TaskListTopBar() },
        bottomBar = { TaskListBottomBar(onNavigateToCalendar) },
        floatingActionButton = { AddTaskFab(onNavigateToAddTask) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            TaskList(
                tasksGroupedByDate = tasksGroupedByDate,
                today = today,
                viewModel = viewModel,
                onNavigateToEditTask = onNavigateToEditTask,
                snackbarHostState = snackbarHostState,
                scope = scope,
                paddingValues = paddingValues
            )
            
            // Botón de prueba para notificaciones - MEJORADO PARA MAYOR VISIBILIDAD
            Button(
                onClick = { showTestNotificationDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.9f)
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3D00) // Naranjo intenso para mayor visibilidad
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

/**
 * Lista de tareas agrupadas por fecha
 */
@Composable
private fun TaskList(
    tasksGroupedByDate: Map<LocalDate, List<Task>>,
    today: LocalDate,
    viewModel: TaskPresenter,
    onNavigateToEditTask: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val todayAndFutureTasks = tasksGroupedByDate.filter { (date, _) -> 
            !date.isBefore(today)
        }
        
        todayAndFutureTasks.forEach { (date, tasks) ->
            item {
                Text(
                    viewModel.formatDate(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            items(tasks, key = { it.id }) { task ->
                SwipeableTaskItem(
                    task = task,
                    onTaskCheckedChange = { isCompleted ->
                        viewModel.updateTaskStatus(task.id, isCompleted)
                    },
                    onDelete = {
                        scope.launch {
                            // Guardar la tarea localmente antes de eliminarla
                            val deletedTask = task
                            
                            // Eliminar la tarea
                            viewModel.deleteTask(task.id)
                            
                            // Mostrar el Snackbar
                            val result = snackbarHostState.showSnackbar(
                                message = "Has eliminado una tarea, si te has equivocado dale recuperar",
                                actionLabel = "Recuperar",
                                duration = SnackbarDuration.Short
                            )
                            
                            // Si se hace clic en "Recuperar", recuperar la tarea
                            if (result == SnackbarResult.ActionPerformed) {
                                // Recuperar la tarea usando la variable local
                                viewModel.undoDeleteTask(deletedTask)
                            }
                        }
                    },
                    onEdit = {
                        onNavigateToEditTask(task.id)
                    }
                )
            }
        }

        if (todayAndFutureTasks.isEmpty()) {
            item {
                EmptyTasksMessage()
            }
        }
    }
}