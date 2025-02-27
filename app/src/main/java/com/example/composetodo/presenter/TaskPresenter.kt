package com.example.composetodo.presenter

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.composetodo.model.notification.NotificationBuilder

class TaskPresenter(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TaskPresenter"
    }

    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    private val locale = Locale("es", "ES")
    private val notificationPresenter = NotificationPresenter(application)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

    val allTasksGroupedByDate: Flow<Map<LocalDate, List<Task>>> = taskDao.getAllTasks()
        .map { tasks ->
            tasks.groupBy { it.scheduledDate }
                .toSortedMap()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: Flow<List<Task>> = _selectedDate.flatMapLatest { date ->
        taskDao.getTasksByDate(date)
    }

    private val _upcomingReminders = mutableStateOf<List<Task>>(emptyList())
    val upcomingReminders: State<List<Task>> = _upcomingReminders

    private val _events = mutableStateOf<TaskEvent?>(null)
    val events: State<TaskEvent?> = _events

    fun formatDate(date: LocalDate): String {
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val dayOfMonth = date.dayOfMonth
        val month = date.month.getDisplayName(TextStyle.FULL, locale)
        return "$dayOfWeek, $dayOfMonth De $month"
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun getTaskById(taskId: Int) {
        viewModelScope.launch {
            taskDao.getTaskById(taskId)?.let { task ->
                _selectedTask.value = task
            }
        }
    }

    fun clearSelectedTask() {
        _selectedTask.value = null
    }

    fun addTask(
        title: String,
        description: String = "",
        priority: Priority = Priority.MEDIA,
        scheduledDate: LocalDate = LocalDate.now(),
        reminderDateTime: LocalDateTime? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Añadiendo nueva tarea: $title, recordatorio: $reminderDateTime")
                
                val task = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    date = LocalDateTime.now(),
                    scheduledDate = scheduledDate,
                    reminderDateTime = reminderDateTime
                )
                
                // Insertar la tarea en la base de datos
                val taskId = taskDao.addTaskAndGetId(task)
                Log.d(TAG, "Tarea añadida con ID: $taskId")
                
                // Programar notificación si tiene recordatorio
                if (reminderDateTime != null) {
                    Log.d(TAG, "Programando notificación para la tarea ID: $taskId")
                    
                    // Obtener la tarea recién creada con su ID asignado
                    val createdTask = taskDao.getTaskById(taskId.toInt())
                    
                    if (createdTask != null) {
                        Log.d(TAG, "Tarea recuperada correctamente, programando notificación")
                        notificationPresenter.scheduleNotification(createdTask)
                    } else {
                        Log.e(TAG, "No se pudo recuperar la tarea recién creada, usando ID temporal")
                        
                        // Como fallback, usar un ID temporal
                        val taskWithReminder = Task(
                            id = taskId.toInt(),
                            title = title,
                            description = description,
                            priority = priority,
                            date = LocalDateTime.now(),
                            scheduledDate = scheduledDate,
                            reminderDateTime = reminderDateTime
                        )
                        notificationPresenter.scheduleNotification(taskWithReminder)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir tarea", e)
            }
        }
    }

    fun updateExistingTask(
        taskId: Int,
        title: String,
        description: String,
        priority: Priority,
        scheduledDate: LocalDate,
        reminderDateTime: LocalDateTime?
    ) {
        viewModelScope.launch {
            val currentTask = taskDao.getTaskById(taskId)
            if (currentTask != null) {
                val updatedTask = currentTask.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    scheduledDate = scheduledDate,
                    reminderDateTime = reminderDateTime
                )
                taskDao.updateTask(updatedTask)
                
                // Cancelar notificación anterior y programar nueva si es necesario
                notificationPresenter.cancelNotification(taskId)
                if (reminderDateTime != null) {
                    notificationPresenter.scheduleNotification(updatedTask)
                }
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            // Cancelar cualquier notificación programada
            notificationPresenter.cancelNotification(taskId)
            taskDao.deleteTaskById(taskId)
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch {
            // Log para debug
            android.util.Log.d("TaskPresenter", "Recuperando tarea: ${task.title}")
            
            // Utilizamos copy con id=0 para permitir que Room genere un nuevo ID
            taskDao.addTask(task.copy(id = 0))
            
            // Reprogramar notificación si tenía recordatorio
            task.reminderDateTime?.let {
                // Como no tenemos el nuevo ID, creamos una copia con un ID temporal
                val tempTask = task.copy(id = System.currentTimeMillis().toInt())
                notificationPresenter.scheduleNotification(tempTask)
            }
            
            // Log para confirmación
            android.util.Log.d("TaskPresenter", "Tarea recuperada con éxito: ${task.title}")
        }
    }

    fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            taskDao.updateTaskStatus(taskId, isCompleted)
            
            // Si la tarea está completada, cancelar la notificación
            if (isCompleted) {
                notificationPresenter.cancelNotification(taskId)
            } else {
                // Si se marca como no completada y tiene recordatorio, reprogramar
                taskDao.getTaskById(taskId)?.let { task ->
                    if (task.reminderDateTime != null) {
                        notificationPresenter.scheduleNotification(task)
                    }
                }
            }
        }
    }
    
    fun updateTaskReminder(taskId: Int, reminderDateTime: LocalDateTime?) {
        viewModelScope.launch {
            taskDao.updateTaskReminder(taskId, reminderDateTime)
            
            // Cancelar notificación existente
            notificationPresenter.cancelNotification(taskId)
            
            // Programar nueva notificación si es necesario
            if (reminderDateTime != null) {
                taskDao.getTaskById(taskId)?.let { task ->
                    notificationPresenter.scheduleNotification(task)
                }
            }
        }
    }
    
    /**
     * Comprueba si la aplicación tiene permiso para mostrar notificaciones
     */
    fun hasNotificationPermission(): Boolean {
        val context = getApplication<Application>().applicationContext
        val notificationBuilder = NotificationBuilder(context)
        return notificationBuilder.hasNotificationPermission()
    }

    /**
     * Obtiene tareas con recordatorios en un rango de tiempo específico
     * Este método consulta la base de datos directamente
     */
    suspend fun getTasksWithRemindersInRange(start: LocalDateTime, end: LocalDateTime): List<Task> {
        return try {
            withContext(viewModelScope.coroutineContext) {
                // Obtenemos todas las tareas de la base de datos primero
                val allTasks = taskDao.getAllTasks().first()
                
                // Filtramos las tareas con recordatorios en el rango especificado
                allTasks.filter { task ->
                    !task.isCompleted &&
                    task.reminderDateTime != null &&
                    task.reminderDateTime.isAfter(start) &&
                    task.reminderDateTime.isBefore(end)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tareas con recordatorios en rango", e)
            emptyList()
        }
    }

    /**
     * Notifica al usuario de recordatorios próximos cuando está en la aplicación
     */
    fun notifyUpcomingReminders(upcomingTasks: List<Task>) {
        try {
            Log.d(TAG, "Notificando ${upcomingTasks.size} recordatorios próximos")
            
            // Actualizamos el estado para que la UI pueda mostrar alguna indicación
            _upcomingReminders.value = upcomingTasks
            
            // Emitimos un evento efímero si es necesario
            _events.value = TaskEvent.UpcomingReminders(upcomingTasks)
        } catch (e: Exception) {
            Log.e(TAG, "Error al notificar recordatorios próximos", e)
        }
    }

    /**
     * Método para probar las notificaciones directamente
     * Crea una notificación de prueba que se mostrará inmediatamente
     */
    fun testNotification(testTask: Task) {
        Log.d(TAG, "Probando notificación con tarea temporal: ${testTask.title}, recordatorio: ${testTask.reminderDateTime}")
        
        viewModelScope.launch {
            try {
                // Mostrar la notificación inmediatamente sin usar AlarmManager
                withContext(Dispatchers.Main) {
                    val context = getApplication<Application>().applicationContext
                    val notificationBuilder = NotificationBuilder(context)
                    
                    // Verificar permisos
                    val hasPermission = notificationBuilder.hasNotificationPermission()
                    Log.d(TAG, "Permiso de notificaciones: $hasPermission")
                    
                    if (hasPermission) {
                        // Mostrar notificación directamente
                        notificationBuilder.showTaskNotification(testTask)
                        Log.d(TAG, "Notificación de prueba mostrada directamente")
                    } else {
                        Log.e(TAG, "No se pudo mostrar la notificación por falta de permisos")
                    }
                }
                
                // También programar la notificación con AlarmManager como respaldo
                if (testTask.reminderDateTime != null) {
                    notificationPresenter.scheduleNotification(testTask)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al probar notificación", e)
            }
        }
    }
}

sealed class TaskEvent {
    data class UpcomingReminders(val tasks: List<Task>) : TaskEvent()
    // Otros eventos que puedan existir
} 