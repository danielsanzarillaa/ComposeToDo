package com.example.composetodo.data.local

import androidx.room.*
import com.example.composetodo.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            CASE priority
                WHEN 'ALTA' THEN 1
                WHEN 'MEDIA' THEN 2
                WHEN 'BAJA' THEN 3
            END,
            date DESC,
            isCompleted ASC
    """)
    fun getAllTasks(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE date >= :startOfDay AND date < :endOfDay
        ORDER BY 
            CASE priority
                WHEN 'ALTA' THEN 1
                WHEN 'MEDIA' THEN 2
                WHEN 'BAJA' THEN 3
            END
    """)
    fun getTasksForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean)
} 