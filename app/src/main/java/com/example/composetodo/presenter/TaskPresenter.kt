package com.example.composetodo.presenter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.composetodo.data.local.TaskDatabase
import com.example.composetodo.model.Priority
import com.example.composetodo.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class TaskPresenter(application: Application) : AndroidViewModel(application) {
    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    private val locale = Locale("es", "ES")

    val taskDates: Flow<List<LocalDate>> = taskDao.getTaskDates()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val allTasksGroupedByDate: Flow<Map<LocalDate, List<Task>>> = taskDao.getAllTasks()
        .map { tasks ->
            tasks.groupBy { it.scheduledDate }
                .toSortedMap()
        }

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

    fun addTask(
        title: String,
        description: String = "",
        priority: Priority = Priority.MEDIA,
        scheduledDate: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                date = LocalDateTime.now(),
                scheduledDate = scheduledDate
            )
            taskDao.addTask(task)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.addTask(task.copy(id = 0))
        }
    }

    fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            taskDao.updateTaskStatus(taskId, isCompleted)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }
} 