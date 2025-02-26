package com.example.composetodo.view.components.TaskListComponents

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Botón flotante para añadir una nueva tarea.
 * 
 * @param onNavigateToAddTask Función para navegar a la pantalla de añadir tarea
 */
@Composable
fun AddTaskFab(onNavigateToAddTask: () -> Unit) {
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
} 