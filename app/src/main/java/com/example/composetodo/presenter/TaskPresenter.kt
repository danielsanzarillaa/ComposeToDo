package com.example.composetodo.presenter

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import com.example.composetodo.model.notification.NotificationBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*

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
        .map { tasks -> tasks.groupBy { it.scheduledDate }.toSortedMap() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelectedDate: Flow<List<Task>> = _selectedDate.flatMapLatest { date ->
        taskDao.getTasksByDate(date)
    }

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
                
                val taskId = taskDao.addTaskAndGetId(task)
                Log.d(TAG, "Tarea añadida con ID: $taskId")
                
                if (reminderDateTime != null) {
                    scheduleNotificationForTask(taskId.toInt(), title, description, scheduledDate, reminderDateTime)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir tarea", e)
            }
        }
    }
    
    private fun scheduleNotificationForTask(
        taskId: Int,
        title: String,
        description: String,
        scheduledDate: LocalDate,
        reminderDateTime: LocalDateTime
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Programando notificación para la tarea ID: $taskId")
                
                val createdTask = taskDao.getTaskById(taskId)
                
                if (createdTask != null) {
                    Log.d(TAG, "Programando notificación para tarea existente")
                    notificationPresenter.scheduleNotification(createdTask)
                } else {
                    Log.d(TAG, "Usando tarea temporal para notificación")
                    
                    val taskWithReminder = Task(
                        id = taskId,
                        title = title,
                        description = description,
                        priority = Priority.MEDIA,
                        date = LocalDateTime.now(),
                        scheduledDate = scheduledDate,
                        reminderDateTime = reminderDateTime
                    )
                    notificationPresenter.scheduleNotification(taskWithReminder)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al programar notificación", e)
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
            try {
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
                    
                    notificationPresenter.cancelNotification(taskId)
                    if (reminderDateTime != null) {
                        notificationPresenter.scheduleNotification(updatedTask)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar tarea", e)
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                notificationPresenter.cancelNotification(taskId)
                taskDao.deleteTaskById(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar tarea", e)
            }
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Recuperando tarea: ${task.title}")
                
                val newTaskId = taskDao.addTaskAndGetId(task.copy(id = 0))
                
                task.reminderDateTime?.let {
                    val newTask = task.copy(id = newTaskId.toInt())
                    notificationPresenter.scheduleNotification(newTask)
                }
                
                Log.d(TAG, "Tarea recuperada con éxito: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar tarea eliminada", e)
            }
        }
    }

    fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                taskDao.updateTaskStatus(taskId, isCompleted)
                
                if (isCompleted) {
                    notificationPresenter.cancelNotification(taskId)
                } else {
                    taskDao.getTaskById(taskId)?.let { task ->
                        task.reminderDateTime?.let {
                            notificationPresenter.scheduleNotification(task)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar estado de tarea", e)
            }
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        val context = getApplication<Application>().applicationContext
        val notificationBuilder = NotificationBuilder(context)
        return notificationBuilder.hasNotificationPermission()
    }

    fun testNotification(testTask: Task) {
        Log.d(TAG, "Probando notificación con tarea temporal: ${testTask.title}")
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    val context = getApplication<Application>().applicationContext
                    val notificationBuilder = NotificationBuilder(context)
                    
                    if (notificationBuilder.hasNotificationPermission()) {
                        notificationBuilder.showTaskNotification(testTask)
                        Log.d(TAG, "Notificación de prueba mostrada correctamente")
                    } else {
                        Log.e(TAG, "No se pudo mostrar la notificación por falta de permisos")
                    }
                }
                
                testTask.reminderDateTime?.let {
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
} 