package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plantId"])
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Long,
    val type: String, // "WATERING" or "FERTILIZING"
    val dueDate: Long,
    val isCompleted: Boolean = false
)
