package com.example.nothingplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val isPurchased: Boolean = false
)
