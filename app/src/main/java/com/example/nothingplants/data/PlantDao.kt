package com.example.nothingplants.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants")
    suspend fun getAllPlantsSync(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :plantId LIMIT 1")
    fun getPlantById(plantId: Long): Flow<Plant>

    @Query("SELECT * FROM plants WHERE id = :plantId LIMIT 1")
    suspend fun getPlantByIdSync(plantId: Long): Plant?

    @Insert
    fun insertPlant(plant: Plant): Long

    @androidx.room.Update
    fun updatePlant(plant: Plant)

    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getWateringLogsForPlant(plantId: Long): Flow<List<WateringLog>>

    @Query("SELECT * FROM watering_logs")
    fun getAllWateringLogs(): Flow<List<WateringLog>>

    @Query("SELECT * FROM watering_logs WHERE id = :id LIMIT 1")
    suspend fun getWateringLogByIdSync(id: Long): WateringLog?

    @Insert
    fun insertWateringLog(log: WateringLog): Long

    @androidx.room.Update
    fun updateWateringLog(log: WateringLog)

    @androidx.room.Delete
    fun deleteWateringLog(log: WateringLog)

    @androidx.room.Delete
    fun deletePlant(plant: Plant)

    @Query("DELETE FROM plants")
    suspend fun deleteAllPlants(): Int

    @Query("DELETE FROM watering_logs")
    suspend fun deleteAllWateringLogs(): Int
}
