package dev.tuandoan.tasktracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun observeSubtasks(taskId: Long): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    suspend fun getSubtasks(taskId: Long): List<Subtask>

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getSubtaskById(id: Long): Subtask?

    @Insert
    suspend fun insertSubtask(subtask: Subtask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subtasks: List<Subtask>)

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    @Query("UPDATE subtasks SET isCompleted = 0 WHERE taskId = :taskId")
    suspend fun resetCompletionForTask(taskId: Long)

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun countForTask(taskId: Long): Int

    @Query("SELECT * FROM subtasks ORDER BY taskId ASC, sortOrder ASC, id ASC")
    suspend fun getAllSubtasks(): List<Subtask>
}
