package com.example.nothingplants.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingInventoryDao {
    // Shopping List
    @Query("SELECT * FROM shopping_list ORDER BY isPurchased ASC, id DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShoppingItem(item: ShoppingItem): Long

    @Update
    fun updateShoppingItem(item: ShoppingItem)

    @Delete
    fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_list WHERE isPurchased = 1")
    suspend fun clearPurchasedItems(): Int

    // Inventory
    @Query("SELECT * FROM inventory ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInventoryItem(item: InventoryItem): Long

    @Update
    fun updateInventoryItem(item: InventoryItem)

    @Delete
    fun deleteInventoryItem(item: InventoryItem)
    
    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getInventoryItemById(id: Long): InventoryItem?
}
