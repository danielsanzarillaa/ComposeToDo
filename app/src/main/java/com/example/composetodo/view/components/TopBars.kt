package com.example.composetodo.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Barra superior genérica con título y botón de navegación hacia atrás.
 * Componente reutilizable para diferentes pantallas de la aplicación.
 *
 * @param title Título que se mostrará en la barra superior
 * @param onNavigateBack Función que se ejecutará al pulsar el botón de navegación
 * @param actions Contenido opcional para mostrar acciones adicionales en la barra superior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { 
            Text(
                text = title, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Volver"
                )
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

/**
 * Barra superior específica para la pantalla de calendario.
 *
 * @param onNavigateBack Función que se ejecutará al pulsar el botón de navegación
 */
@Composable
fun CalendarTopBar(onNavigateBack: () -> Unit) {
    GenericTopBar(
        title = "Calendario",
        onNavigateBack = onNavigateBack
    )
} 