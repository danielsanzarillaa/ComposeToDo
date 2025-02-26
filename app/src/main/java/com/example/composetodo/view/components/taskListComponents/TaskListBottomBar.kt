package com.example.composetodo.view.components.TaskListComponents

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Barra inferior de navegación para la pantalla de lista de tareas.
 * Permite navegar entre la lista de tareas y el calendario.
 *
 * @param onNavigateToCalendar Función para navegar a la pantalla de calendario
 */
@Composable
fun TaskListBottomBar(onNavigateToCalendar: () -> Unit) {
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
} 