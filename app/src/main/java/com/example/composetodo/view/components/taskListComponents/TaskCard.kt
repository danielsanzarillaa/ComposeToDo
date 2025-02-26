package com.example.composetodo.view.components.TaskListComponents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composetodo.model.Task

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