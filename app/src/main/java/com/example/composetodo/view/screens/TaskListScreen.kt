package com.example.composetodo.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import com.example.composetodo.presenter.TaskPresenter
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = { 
            LargeTopAppBar(
                title = { 
                    Text(
                        "Mis Tareas",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tareas"
                        )
                    },
                    label = { Text("Tareas") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCalendar,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendario"
                        )
                    },
                    label = { Text("Calendario") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Añadir tarea",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tasksGroupedByDate.forEach { (date, tasks) ->
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
                                lastDeletedTask = task
                                viewModel.deleteTask(task.id)
                                val result = snackbarHostState.showSnackbar(
                                    message = "Tarea eliminada",
                                    actionLabel = "Deshacer",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    lastDeletedTask?.let { viewModel.undoDeleteTask(it) }
                                }
                                lastDeletedTask = null
                            }
                        },
                        onEdit = {
                            onNavigateToEditTask(task.id)
                        }
                    )
                }
            }

            if (tasksGroupedByDate.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay tareas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: Task,
    onTaskCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                val alpha = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = alpha)
                )
            }
        },
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (task.priority) {
                            Priority.ALTA -> Color(0xFFFFEDED)
                            Priority.MEDIA -> Color(0xFFFFF8E1)
                            Priority.BAJA -> Color(0xFFE8F5E9)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isCompleted) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (task.description.isNotEmpty()) {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            imageVector = if (expanded) 
                                                Icons.Default.KeyboardArrowUp 
                                            else 
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expanded) 
                                                "Ocultar descripción" 
                                            else 
                                                "Mostrar descripción",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                IconButton(onClick = onEdit) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar tarea",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                IconButton(onClick = { onTaskCheckedChange(!task.isCompleted) }) {
                                    Icon(
                                        imageVector = if (task.isCompleted) 
                                            Icons.Filled.CheckCircle 
                                        else 
                                            Icons.Outlined.CheckCircle,
                                        contentDescription = "Completar tarea",
                                        tint = if (task.isCompleted)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        
                        if (expanded && task.description.isNotEmpty()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}