package com.example.composetodo.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composetodo.model.Task
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.components.Calendar
import com.example.composetodo.view.components.CalendarTopBar
import com.example.composetodo.view.components.TaskByDateCalendarComponent
import java.time.LocalDate
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = { CalendarTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
    }
} 