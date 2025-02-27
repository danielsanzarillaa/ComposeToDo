package com.example.composetodo.data.local

import androidx.room.*
import com.example.composetodo.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface TaskDao {
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            scheduledDate ASC,
            CASE priority
                WHEN 'ALTA' THEN 1
                WHEN 'MEDIA' THEN 2
                WHEN 'BAJA' THEN 3
            END,
            isCompleted ASC,
            date DESC
    """)
    fun getAllTasks(): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks 
        WHERE scheduledDate = :date
        ORDER BY 
            CASE priority
                WHEN 'ALTA' THEN 1
                WHEN 'MEDIA' THEN 2
                WHEN 'BAJA' THEN 3
            END,
            date DESC,
            isCompleted ASC
    """)
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("""
        SELECT DISTINCT scheduledDate 
        FROM tasks 
        WHERE scheduledDate >= :startDate
        ORDER BY scheduledDate ASC
    """)
    fun getTaskDates(startDate: LocalDate = LocalDate.now()): Flow<List<LocalDate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTask(task: Task)

    /**
     * Inserta una tarea y devuelve el ID generado
     * @param task La tarea a insertar
     * @return El ID generado para la tarea
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTaskAndGetId(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean)
    @Query("UPDATE tasks SET reminderDateTime = :reminderDateTime WHERE id = :taskId")
    suspend fun updateTaskReminder(taskId: Int, reminderDateTime: LocalDateTime?)

} 