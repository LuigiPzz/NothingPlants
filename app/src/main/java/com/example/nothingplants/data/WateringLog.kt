package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watering_logs")
data class WateringLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Long,
    val timestamp: Long,
    val aiHealthScore: Int? = null,
    val aiNotes: String? = null,
    val imagePath: String? = null,
    val suggestedNextWateringDays: Int? = null,
    val logType: String = "WATERING",
    val luxValue: Float? = null,
    val compassDirection: String? = null,
    val newPotDiameter: Int? = null
)
