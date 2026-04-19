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
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id IN (:ids) AND isArchived = 0")
    suspend fun markCompleted(ids: List<Long>, completedAt: Long)

    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE id IN (:ids) AND isArchived = 0")
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

    // Stats queries (exclude archived tasks)
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isArchived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND isArchived = 0")
    fun observeCompletedCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND isArchived = 0 AND completedAt >= :startOfDayMillis AND completedAt < :endOfDayMillis",
    )
    fun observeCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isArchived = 0 AND dueAt >= :startOfDayMillis AND dueAt < :endOfDayMillis",
    )
    fun observeDueTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isArchived = 0 AND dueAt < :nowMillis")
    fun observeOverdueCount(nowMillis: Long): Flow<Int>

    // Weekly breakdown query (completed tasks grouped by day)
    @Query(
        "SELECT date(completedAt / 1000, 'unixepoch', 'localtime') AS date, COUNT(*) AS count " +
            "FROM tasks WHERE isCompleted = 1 AND isArchived = 0 " +
            "AND completedAt BETWEEN :startMillis AND :endMillis " +
            "GROUP BY date ORDER BY date ASC",
    )
    fun observeCompletedCountPerDay(startMillis: Long, endMillis: Long): Flow<List<DailyCount>>

    // Recurrence queries
    @Query(
        "SELECT * FROM tasks WHERE parentRecurringTaskId = :parentId AND isCompleted = 0 AND isArchived = 0 ORDER BY createdAt DESC LIMIT 1",
    )
    suspend fun getLatestGeneratedTask(parentId: Long): Task?

    // Streak queries
    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 1 AND (id = :rootId OR parentRecurringTaskId = :rootId) ORDER BY completedAt ASC",
    )
    suspend fun getCompletedTasksByChain(rootId: Long): List<Task>

    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 1 " +
            "AND (id IN (:rootIds) OR parentRecurringTaskId IN (:rootIds)) " +
            "ORDER BY completedAt ASC",
    )
    suspend fun getCompletedTasksForChains(rootIds: List<Long>): List<Task>

    @Query(
        "SELECT DISTINCT " +
            "CASE WHEN parentRecurringTaskId IS NOT NULL " +
            "THEN parentRecurringTaskId ELSE id END AS rootId " +
            "FROM tasks " +
            "WHERE recurrenceType != 0 AND isArchived = 0 AND isCompleted = 0",
    )
    suspend fun getActiveRecurringRootIds(): List<Long>

    // Widget query
    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 0 AND isArchived = 0 " +
            "ORDER BY isPinned DESC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC " +
            "LIMIT :limit",
    )
    suspend fun getWidgetTasks(limit: Int): List<Task>

    // Tag management operations
    @Query(
        "SELECT tag, tagColor, COUNT(*) AS taskCount FROM tasks " +
            "WHERE tag IS NOT NULL AND tag != '' AND isArchived = 0 " +
            "GROUP BY tag ORDER BY tag ASC",
    )
    fun getDistinctTagsWithCount(): Flow<List<TagInfo>>

    @Query("UPDATE tasks SET tag = :newName WHERE tag = :oldName")
    suspend fun updateTagName(oldName: String, newName: String)

    @Query("UPDATE tasks SET tag = NULL, tagColor = NULL WHERE tag = :tagName")
    suspend fun clearTag(tagName: String)

    @Query("UPDATE tasks SET tagColor = :color WHERE tag = :tagName")
    suspend fun updateTagColor(tagName: String, color: String?)

    @Query("SELECT tagColor FROM tasks WHERE tag = :tagName AND tagColor IS NOT NULL LIMIT 1")
    suspend fun getTagColor(tagName: String): String?

    // Backup operations
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksIncludingArchived(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
