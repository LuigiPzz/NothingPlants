package com.example.nothingplants.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SyncRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val plantDao = db.plantDao()
    private val reminderDao = db.reminderDao()
    private val driveService = GoogleDriveService(context)

    data class SyncSummary(
        val plantCount: Int,
        val photoCount: Int,
        val lastSyncTimestamp: Long
    )

    suspend fun upload(account: GoogleSignInAccount): Result<Unit> = runCatching {
        val plants = plantDao.getAllPlants().first()
        val reminders = reminderDao.getActiveReminders().first()
        
        val wateringLogs = mutableListOf<WateringLog>()
        plants.forEach { plant ->
            val logs = plantDao.getWateringLogsForPlant(plant.id).first()
            wateringLogs.addAll(logs)
        }
        
        val photoFilesMap = mutableMapOf<String, String>() // filename -> fileId
        val filesToUpload = mutableSetOf<File>()
        
        plants.forEach { plant ->
            plant.imageUri?.let { path ->
                val f = File(path)
                if (f.exists()) filesToUpload.add(f)
            }
        }
        wateringLogs.forEach { log ->
            log.imagePath?.let { path ->
                val f = File(path)
                if (f.exists()) filesToUpload.add(f)
            }
        }
        
        // Carica ciascuna foto su Drive
        filesToUpload.forEach { file ->
            val fileId = driveService.uploadFile(account, file, "image/jpeg")
            photoFilesMap[file.name] = fileId
        }
        
        // Costruisci il JSON del backup
        val backupObj = org.json.JSONObject()
        backupObj.put("version", 1)
        backupObj.put("lastSyncTimestamp", System.currentTimeMillis())
        
        val plantsArray = org.json.JSONArray()
        plants.forEach { plant ->
            val pObj = org.json.JSONObject()
            pObj.put("id", plant.id)
            pObj.put("name", plant.name)
            pObj.put("species", plant.species)
            pObj.put("adoptionDate", plant.adoptionDate)
            pObj.put("room", plant.room ?: "")
            pObj.put("imageFilename", plant.imageUri?.let { File(it).name } ?: "")
            pObj.put("originalImageFilename", plant.originalImageUri?.let { File(it).name } ?: "")
            plantsArray.put(pObj)
        }
        backupObj.put("plants", plantsArray)
        
        val remindersArray = org.json.JSONArray()
        reminders.forEach { reminder ->
            val rObj = org.json.JSONObject()
            rObj.put("id", reminder.id)
            rObj.put("plantId", reminder.plantId)
            rObj.put("type", reminder.type)
            rObj.put("dueDate", reminder.dueDate)
            rObj.put("isCompleted", reminder.isCompleted)
            remindersArray.put(rObj)
        }
        backupObj.put("reminders", remindersArray)
        
        val logsArray = org.json.JSONArray()
        wateringLogs.forEach { log ->
            val lObj = org.json.JSONObject()
            lObj.put("id", log.id)
            lObj.put("logType", log.logType)
            lObj.put("plantId", log.plantId)
            lObj.put("timestamp", log.timestamp)
            lObj.put("imageFilename", log.imagePath?.let { File(it).name } ?: "")
            lObj.put("aiHealthScore", log.aiHealthScore ?: -1)
            lObj.put("aiNotes", log.aiNotes ?: "")
            lObj.put("suggestedNextWateringDays", log.suggestedNextWateringDays ?: -1)
            logsArray.put(lObj)
        }
        backupObj.put("wateringLogs", logsArray)
        
        val photosObj = org.json.JSONObject()
        photoFilesMap.forEach { (name, id) ->
            photosObj.put(name, id)
        }
        backupObj.put("photoFiles", photosObj)
        
        driveService.uploadSyncFile(account, backupObj.toString())
        
        // Pulizia file orfani
        driveService.cleanupObsoletePhotos(account, photoFilesMap.keys)
    }

    suspend fun downloadAndApply(account: GoogleSignInAccount): Result<Int> = runCatching {
        val json = driveService.downloadSyncFile(account) ?: return Result.success(0)
        val backupObj = org.json.JSONObject(json)
        
        // Mappa delle foto
        val photosObj = backupObj.optJSONObject("photoFiles") ?: org.json.JSONObject()
        val photosMap = mutableMapOf<String, String>()
        photosObj.keys().forEach { name ->
            photosMap[name] = photosObj.getString(name)
        }
        
        // Scarica le foto mancanti locali
        photosMap.forEach { (filename, fileId) ->
            val destFile = File(context.filesDir, filename)
            if (!destFile.exists()) {
                try {
                    driveService.downloadFile(account, fileId, destFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        val plantsArray = backupObj.optJSONArray("plants") ?: org.json.JSONArray()
        val remindersArray = backupObj.optJSONArray("reminders") ?: org.json.JSONArray()
        val logsArray = backupObj.optJSONArray("wateringLogs") ?: org.json.JSONArray()
        
        // Resetta database locale
        plantDao.deleteAllPlants()
        plantDao.deleteAllWateringLogs()
        reminderDao.deleteAllReminders()
        
        // Ripristina piante
        for (i in 0 until plantsArray.length()) {
            val pObj = plantsArray.getJSONObject(i)
            val id = pObj.getLong("id")
            val name = pObj.getString("name")
            val species = pObj.getString("species")
            val adoptionDate = pObj.getLong("adoptionDate")
            val roomVal = pObj.optString("room", "")
            val room = if (roomVal.isBlank()) null else roomVal
            val imageFilename = pObj.optString("imageFilename", "")
            val imageUri = if (imageFilename.isBlank()) null else File(context.filesDir, imageFilename).absolutePath
            val originalImageFilename = pObj.optString("originalImageFilename", "")
            val originalImageUri = if (originalImageFilename.isBlank()) null else File(context.filesDir, originalImageFilename).absolutePath
            
            val plant = Plant(
                id = id,
                name = name,
                species = species,
                imageUri = imageUri,
                adoptionDate = adoptionDate,
                aiSummary = null,
                room = room,
                originalImageUri = originalImageUri
            )
            plantDao.insertPlant(plant)
        }
        
        // Ripristina log
        for (i in 0 until logsArray.length()) {
            val lObj = logsArray.getJSONObject(i)
            val id = lObj.getLong("id")
            val logType = lObj.optString("logType", "WATERING")
            val plantId = lObj.getLong("plantId")
            val timestamp = lObj.getLong("timestamp")
            val imageFilename = lObj.optString("imageFilename", "")
            val imagePath = if (imageFilename.isBlank()) null else File(context.filesDir, imageFilename).absolutePath
            val scoreVal = lObj.optInt("aiHealthScore", -1)
            val aiHealthScore = if (scoreVal == -1) null else scoreVal
            val notesVal = lObj.optString("aiNotes", "")
            val aiNotes = if (notesVal.isBlank()) null else notesVal
            val daysVal = lObj.optInt("suggestedNextWateringDays", -1)
            val suggestedNextWateringDays = if (daysVal == -1) null else daysVal
            
            val log = WateringLog(
                id = id,
                logType = logType,
                plantId = plantId,
                timestamp = timestamp,
                imagePath = imagePath,
                aiHealthScore = aiHealthScore,
                aiNotes = aiNotes,
                suggestedNextWateringDays = suggestedNextWateringDays
            )
            plantDao.insertWateringLog(log)
        }
        
        // Ripristina promemoria
        for (i in 0 until remindersArray.length()) {
            val rObj = remindersArray.getJSONObject(i)
            val id = rObj.getLong("id")
            val plantId = rObj.getLong("plantId")
            val type = rObj.getString("type")
            val dueDate = rObj.getLong("dueDate")
            val isCompleted = rObj.getBoolean("isCompleted")
            
            val reminder = Reminder(
                id = id,
                plantId = plantId,
                type = type,
                dueDate = dueDate,
                isCompleted = isCompleted
            )
            reminderDao.insertReminder(reminder)
        }
        
        // Invalida cache dei concimi
        val prefs = PreferencesManager(context)
        prefs.clearFertilizerSummary()
        
        plantsArray.length()
    }

    suspend fun getRemoteSyncSummary(account: GoogleSignInAccount): Result<SyncSummary?> = runCatching {
        val json = driveService.downloadSyncFile(account) ?: return Result.success(null)
        val backupObj = org.json.JSONObject(json)
        val plantsArray = backupObj.optJSONArray("plants") ?: org.json.JSONArray()
        val photosObj = backupObj.optJSONObject("photoFiles") ?: org.json.JSONObject()
        val lastSync = backupObj.optLong("lastSyncTimestamp", 0L)
        
        SyncSummary(
            plantCount = plantsArray.length(),
            photoCount = photosObj.length(),
            lastSyncTimestamp = lastSync
        )
    }
}
