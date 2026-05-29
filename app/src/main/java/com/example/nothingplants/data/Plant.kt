package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val species: String,
    val imageUri: String? = null,
    val adoptionDate: Long = System.currentTimeMillis(),
    val aiSummary: String? = null,
    val room: String? = null,
    val originalImageUri: String? = null,
    val potDiameter: Int? = null
)
