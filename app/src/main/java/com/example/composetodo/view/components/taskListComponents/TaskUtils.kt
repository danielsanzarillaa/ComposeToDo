package com.example.composetodo.view.components.taskListComponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.composetodo.model.Priority

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