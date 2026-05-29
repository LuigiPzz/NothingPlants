package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val levelPercent: Int = 100,
    val description: String? = null
)
