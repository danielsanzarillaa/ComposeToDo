package com.example.composetodo.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.composetodo.model.Priority
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.components.calendarComponents.Calendar
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val scrollState = rememberScrollState()
    
    // Estado para controlar si se está procesando el guardado
    var isSaving by remember { mutableStateOf(false) }

    // Estados de la tarea
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIA) }
    var selectedDate by remember { mutableStateOf(initialDate ?: today) }
    var reminderDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    
    // Estados temporales para el recordatorio
    var reminderDate by remember { mutableStateOf(today) }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }

    // Estados para los diálogos
    var showDatePicker by remember { mutableStateOf(false) }
    var showDatePickerInDialog by remember { mutableStateOf(false) }
    var showTimePickerInDialog by remember { mutableStateOf(false) }

    val selectedTask by viewModel.selectedTask.collectAsState()

    // Cargar datos si estamos en modo edición
    LaunchedEffect(taskId) {
        if (isEditMode && taskId > 0) {
            viewModel.getTaskById(taskId)
        } else {
            viewModel.clearSelectedTask()
        }
    }

    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            taskTitle = task.title
            taskDescription = task.description
            selectedPriority = task.priority
            selectedDate = task.scheduledDate
            reminderDateTime = task.reminderDateTime
            reminderDateTime?.let {
                reminderDate = it.toLocalDate()
                reminderTime = it.toLocalTime()
            }
        }
    }

    // Diálogo de selección de fecha para la tarea
    if (showDatePicker) {
        Dialog(
            onDismissRequest = { showDatePicker = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Seleccionar fecha", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Calendar(
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            if (!date.isBefore(today)) {
                                selectedDate = date
                                showDatePicker = false
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo para seleccionar fecha del recordatorio
    if (showDatePickerInDialog) {
        Dialog(
            onDismissRequest = { showDatePickerInDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Fecha del recordatorio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Calendar(
                        selectedDate = reminderDate,
                        onDateSelected = { date ->
                            if (!date.isBefore(today)) {
                                reminderDate = date
                                showDatePickerInDialog = false
                                showTimePickerInDialog = true
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDatePickerInDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }

    // Diálogo para seleccionar hora del recordatorio
    if (showTimePickerInDialog) {
        Dialog(
            onDismissRequest = { showTimePickerInDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hora del recordatorio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    val timePickerState = rememberTimePickerState(
                        initialHour = reminderTime.hour,
                        initialMinute = reminderTime.minute,
                        is24Hour = true
                    )
                    
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showTimePickerInDialog = false }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                reminderTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                                reminderDateTime = LocalDateTime.of(reminderDate, reminderTime)
                                showTimePickerInDialog = false
                            }
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Tarea" else "Nueva Tarea", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearSelectedTask(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Título de la tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fecha programada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = viewModel.formatDate(selectedDate))
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar fecha", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recordatorio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (reminderDateTime != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Fecha: ${reminderDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Hora: ${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Row {
                                IconButton(onClick = { showDatePickerInDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Cambiar recordatorio",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { reminderDateTime = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar recordatorio",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showDatePickerInDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir recordatorio")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prioridad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Priority.entries.forEach { priority ->
                    val backgroundColor = when (priority) {
                        Priority.ALTA -> Color(0xFFFEE2E2)
                        Priority.MEDIA -> Color(0xFFFFF8E1)
                        Priority.BAJA -> Color(0xFFE8F5E9)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedPriority = priority },
                        color = backgroundColor,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(priority.name)
                            RadioButton(selected = selectedPriority == priority, onClick = { selectedPriority = priority })
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (taskTitle.isNotBlank() && !selectedDate.isBefore(today) && !isSaving) {
                        isSaving = true // Desactivar el botón mientras se guarda
                        scope.launch {
                            if (isEditMode && taskId > 0) {
                                viewModel.updateExistingTask(taskId, taskTitle, taskDescription, selectedPriority, selectedDate, reminderDateTime)
                            } else {
                                viewModel.addTask(taskTitle, taskDescription, selectedPriority, selectedDate, reminderDateTime)
                            }
                            viewModel.clearSelectedTask()
                            // No restauramos isSaving porque vamos a navegar hacia atrás
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && taskTitle.isNotBlank() && !selectedDate.isBefore(today)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditMode) "Actualizando..." else "Guardando...")
                } else {
                    Text(if (isEditMode) "Actualizar Tarea" else "Guardar Tarea")
                }
            }
            
            // Espacio adicional al final para asegurar que el botón sea visible
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}