package com.example.composetodo.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: Priority,
    val date: LocalDateTime = LocalDateTime.now(),
    val scheduledDate: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false,
    val reminderDateTime: LocalDateTime? = null

)

enum class Priority {
    ALTA, MEDIA, BAJA
} 