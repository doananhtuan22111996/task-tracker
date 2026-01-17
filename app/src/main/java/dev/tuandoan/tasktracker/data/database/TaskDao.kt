package dev.tuandoan.tasktracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Main queries (exclude archived tasks by default)
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    // Archived tasks queries
    @Query("SELECT * FROM tasks WHERE isArchived = 1 ORDER BY archivedAt DESC, createdAt DESC")
    fun getArchivedTasks(): Flow<List<Task>>

    // Bulk operations (exclude archived tasks)
    @Query("UPDATE tasks SET isCompleted = 1 WHERE id IN (:ids) AND isArchived = 0")
    suspend fun markCompleted(ids: List<Long>)

    @Query("UPDATE tasks SET isCompleted = 0 WHERE id IN (:ids) AND isArchived = 0")
    suspend fun markActive(ids: List<Long>)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getTasksByIds(ids: List<Long>): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<Task>)

    // Archive operations
    @Query("UPDATE tasks SET isArchived = :archived, archivedAt = :archivedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, archivedAt: Long?)

    @Query("UPDATE tasks SET isArchived = :archived, archivedAt = :archivedAt WHERE id IN (:ids)")
    suspend fun setArchivedBulk(ids: List<Long>, archived: Boolean, archivedAt: Long?)

    // Hard delete operations (for permanent deletion from archived screen)
    @Query("DELETE FROM tasks WHERE id = :id AND isArchived = 1")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM tasks WHERE id IN (:ids) AND isArchived = 1")
    suspend fun hardDeleteByIds(ids: List<Long>)

    // Pin/Priority operations
    @Query("UPDATE tasks SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE tasks SET priority = :priority WHERE id = :id")
    suspend fun setPriority(id: Long, priority: Int)
}