package com.example.composetodo.view.components.TaskListComponents

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Barra superior específica para la pantalla de lista de tareas.
 * Muestra el título "Mis Tareas" con un estilo grande.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar() {
    LargeTopAppBar(
        title = { 
            Text(
                "Mis Tareas",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
} 