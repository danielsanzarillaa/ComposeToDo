package com.example.composetodo.view.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import com.example.composetodo.presenter.TaskPresenter
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import java.time.LocalDate

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val filteredTasks = tasksGroupedByDate.filter { (date, _) -> 
                !date.isBefore(today)
            }
            
            filteredTasks.forEach { (date, tasks) ->
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

            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay tareas pendientes",
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
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isCompleted) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (task.description.isNotEmpty()) {
                                TextButton(
                                    onClick = { expanded = !expanded },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (expanded) "Ocultar" else "Mostrar descripción",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (expanded) 
                                            Icons.Default.KeyboardArrowUp 
                                        else 
                                            Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar tarea",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { onTaskCheckedChange(!task.isCompleted) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) 
                                        Icons.Filled.CheckCircle 
                                    else 
                                        Icons.Outlined.CheckCircle,
                                    contentDescription = "Completar tarea",
                                    tint = if (task.isCompleted)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = expanded && task.description.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(200)) + 
                                   expandVertically(
                                       animationSpec = tween(300, easing = EaseOutQuad),
                                       expandFrom = Alignment.Top
                                   ),
                            exit = fadeOut(animationSpec = tween(200)) + 
                                  shrinkVertically(
                                      animationSpec = tween(300, easing = EaseInQuad),
                                      shrinkTowards = Alignment.Top
                                  )
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}