package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_history")
data class NotificationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val timestamp: Long
)
