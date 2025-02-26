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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskPresenter,
    onNavigateToAddTask: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToEditTask: (Int) -> Unit
) {
    val tasksGroupedByDate by viewModel.allTasksGroupedByDate.collectAsState(initial = emptyMap())
    var lastDeletedTask by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = LocalDate.now()

    Scaffold(
        topBar = { TaskListTopBar() },
        bottomBar = { TaskListBottomBar(onNavigateToCalendar) },
        floatingActionButton = { AddTaskFab(onNavigateToAddTask) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        TaskList(
            tasksGroupedByDate = tasksGroupedByDate,
            today = today,
            viewModel = viewModel,
            onNavigateToEditTask = onNavigateToEditTask,
            snackbarHostState = snackbarHostState,
            scope = scope,
            lastDeletedTask = lastDeletedTask,
            onTaskDeleted = { task -> lastDeletedTask = task },
            onUndoDelete = { lastDeletedTask = null },
            paddingValues = paddingValues
        )
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
    lastDeletedTask: Task?,
    onTaskDeleted: (Task) -> Unit,
    onUndoDelete: () -> Unit,
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
                            onTaskDeleted(task)
                            viewModel.deleteTask(task.id)
                            val result = snackbarHostState.showSnackbar(
                                message = "Tarea eliminada",
                                actionLabel = "Deshacer",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                lastDeletedTask?.let { viewModel.undoDeleteTask(it) }
                                onUndoDelete()
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