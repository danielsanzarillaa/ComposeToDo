package com.example.composetodo.view.components.CalendarComponents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
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
import java.time.LocalDate

/**
 * Componente que muestra un mensaje cuando no hay tareas para la fecha seleccionada
 */
@Composable
fun EmptyTasksMessage(selectedDate: LocalDate, today: LocalDate) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selectedDate.isBefore(today)) 
                "No hubo tareas para este día"
            else 
                "No hay tareas programadas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Botón para añadir una nueva tarea
 */
@Composable
fun AddTaskButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "Añadir tarea para esta fecha",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Lista de tareas para la fecha seleccionada
 */
@Composable
fun TasksList(
    tasksForDate: List<Task>,
    onTaskCheckedChange: (Int, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tasksForDate, key = { it.id }) { task ->
            SwipeableCalendarTaskItem(
                task = task,
                onTaskCheckedChange = { isCompleted ->
                    onTaskCheckedChange(task.id, isCompleted)
                },
                onDelete = { onDeleteTask(task) },
                onEdit = { onEditTask(task.id) }
            )
        }
    }
}

/**
 * Elemento de tarea deslizable que permite eliminar la tarea deslizando hacia la izquierda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCalendarTaskItem(
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
        backgroundContent = { TaskDismissBackground(dismissState) },
        content = {
            TaskCard(
                task = task,
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTaskCheckedChange = onTaskCheckedChange,
                onEdit = onEdit
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}

/**
 * Fondo que se muestra al deslizar una tarea para eliminarla
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDismissBackground(dismissState: SwipeToDismissBoxState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val alpha = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f
        Icon(
            Icons.Default.Delete,
            contentDescription = "Eliminar tarea",
            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = alpha)
        )
    }
}

/**
 * Tarjeta que muestra la información de una tarea
 */
@Composable
fun TaskCard(
    task: Task,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onTaskCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
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
                containerColor = getPriorityColor(task.priority)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column {
                // Encabezado con título
                TaskHeader(task)
                
                // Fila de acciones
                TaskActions(
                    task = task,
                    expanded = expanded,
                    onExpandChange = onExpandChange,
                    onEdit = onEdit,
                    onTaskCheckedChange = onTaskCheckedChange
                )
                
                // Descripción expandible
                TaskDescription(
                    description = task.description,
                    expanded = expanded
                )
            }
        }
    }
}

/**
 * Encabezado de la tarjeta de tarea que muestra el título
 */
@Composable
private fun TaskHeader(task: Task) {
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
}

/**
 * Fila de acciones para una tarea (mostrar/ocultar descripción, editar, marcar como completada)
 */
@Composable
private fun TaskActions(
    task: Task,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTaskCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón para mostrar/ocultar descripción (solo visible si hay descripción)
        if (task.description.isNotEmpty()) {
            TextButton(
                onClick = { onExpandChange(!expanded) },
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
        
        // Botón para editar tarea
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
        
        // Botón para marcar como completada
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
}

/**
 * Descripción expandible de una tarea
 */
@Composable
private fun TaskDescription(
    description: String,
    expanded: Boolean
) {
    AnimatedVisibility(
        visible = expanded && description.isNotEmpty(),
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
        Column {
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
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Devuelve el color correspondiente a la prioridad de una tarea
 */
@Composable
fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.ALTA -> Color(0xFFFFEDED)
        Priority.MEDIA -> Color(0xFFFFF8E1)
        Priority.BAJA -> Color(0xFFE8F5E9)
    }
} 