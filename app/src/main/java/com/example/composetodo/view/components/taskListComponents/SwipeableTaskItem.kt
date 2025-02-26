package com.example.composetodo.view.components.taskListComponents

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composetodo.model.Task

/**
 * Componente que muestra un elemento de tarea deslizable que permite eliminar
 * la tarea deslizando hacia la izquierda.
 *
 * @param task La tarea a mostrar
 * @param onTaskCheckedChange Callback cuando se cambia el estado de completado de la tarea
 * @param onDelete Callback cuando se elimina la tarea
 * @param onEdit Callback cuando se edita la tarea
 */
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
            imageVector = Icons.Default.Delete,
            contentDescription = "Eliminar tarea",
            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = alpha)
        )
    }
} 