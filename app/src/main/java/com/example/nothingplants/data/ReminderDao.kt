package com.example.nothingplants.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE plantId = :plantId ORDER BY dueDate ASC")
    fun getRemindersForPlant(plantId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueDate <= :timestamp")
    suspend fun getDueRemindersSync(timestamp: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder): Int

    @Query("UPDATE reminders SET isCompleted = 1 WHERE plantId = :plantId AND type = :type AND isCompleted = 0")
    suspend fun markRemindersAsCompleted(plantId: Long, type: String): Int

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY dueDate DESC LIMIT 50")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: Long, completed: Boolean): Int

    @androidx.room.Delete
    suspend fun deleteReminder(reminder: Reminder): Int

    @Query("SELECT * FROM reminders WHERE plantId = :plantId AND type = :type AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveReminderForPlantAndTypeSync(plantId: Long, type: String): Reminder?

    @Query("SELECT * FROM reminders WHERE plantId = :plantId AND type = :type AND isCompleted = 0 LIMIT 1")
    fun getActiveReminderForPlantAndType(plantId: Long, type: String): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderByIdSync(id: Long): Reminder?

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long): Int

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders(): Int

    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllNotificationHistory(): Flow<List<NotificationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationHistory(history: NotificationHistory): Long

    @Query("DELETE FROM notification_history")
    suspend fun deleteAllNotificationHistory(): Int
}
