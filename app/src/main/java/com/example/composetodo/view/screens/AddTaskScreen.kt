package com.example.composetodo.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composetodo.model.Priority
import com.example.composetodo.presenter.TaskPresenter
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TaskPresenter,
    onNavigateBack: () -> Unit,
    initialDate: LocalDate? = null,
    taskId: Int = 0,
    isEditMode: Boolean = false
) {
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    
    // Efecto para cargar la tarea si estamos en modo edición
    LaunchedEffect(taskId) {
        if (isEditMode && taskId > 0) {
            viewModel.getTaskById(taskId)
        } else {
            viewModel.clearSelectedTask()
        }
    }
    
    // Estado para la tarea seleccionada
    val selectedTask by viewModel.selectedTask.collectAsState()
    
    // Estados para los campos del formulario
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIA) }
    var selectedDate by remember { 
        mutableStateOf(
            initialDate?.let { 
                if (it.isBefore(today)) today else it 
            } ?: today
        )
    }
    
    // Efecto para actualizar los campos cuando se carga la tarea en modo edición
    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            taskTitle = task.title
            taskDescription = task.description
            selectedPriority = task.priority
            selectedDate = task.scheduledDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) "Editar Tarea" else "Nueva Tarea", 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedTask()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Título de la tarea") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                minLines = 3,
                maxLines = 5
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Fecha programada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = viewModel.formatDate(selectedDate),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Prioridad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Priority.values().forEach { priority ->
                    val backgroundColor = when (priority) {
                        Priority.ALTA -> Color(0xFFFEE2E2)
                        Priority.MEDIA -> Color(0xFFFFF8E1)
                        Priority.BAJA -> Color(0xFFE8F5E9)
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedPriority = priority },
                        color = backgroundColor,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(priority.name)
                            RadioButton(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (taskTitle.isNotBlank() && !selectedDate.isBefore(today)) {
                        scope.launch {
                            if (isEditMode && taskId > 0) {
                                viewModel.updateExistingTask(
                                    taskId = taskId,
                                    title = taskTitle,
                                    description = taskDescription,
                                    priority = selectedPriority,
                                    scheduledDate = selectedDate
                                )
                            } else {
                                viewModel.addTask(
                                    title = taskTitle,
                                    description = taskDescription,
                                    priority = selectedPriority,
                                    scheduledDate = selectedDate
                                )
                            }
                            viewModel.clearSelectedTask()
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = taskTitle.isNotBlank() && !selectedDate.isBefore(today)
            ) {
                Text(
                    if (isEditMode) "Actualizar Tarea" else "Guardar Tarea", 
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
} 