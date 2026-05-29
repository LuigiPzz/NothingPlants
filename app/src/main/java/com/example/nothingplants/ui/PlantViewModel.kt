package com.example.nothingplants.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingplants.ai.PlantAnalyzer
import com.example.nothingplants.data.AppDatabase
import com.example.nothingplants.data.Plant
import com.example.nothingplants.data.PreferencesManager
import com.example.nothingplants.data.WateringLog
import com.example.nothingplants.data.GoogleDriveService
import com.example.nothingplants.data.SyncRepository
import com.example.nothingplants.data.SyncRepository.SyncSummary
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PlantViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val plantDao = db.plantDao()
    private val reminderDao = db.reminderDao()
    private val shoppingInventoryDao = db.shoppingInventoryDao()
    val preferencesManager = PreferencesManager(application)

    val driveService = GoogleDriveService(application)
    private val syncRepository = SyncRepository(application)

    // Shopping List Flow ed Azioni
    val allShoppingItems: StateFlow<List<com.example.nothingplants.data.ShoppingItem>> = shoppingInventoryDao.getAllShoppingItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addShoppingItem(name: String, quantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank()) {
                val item = com.example.nothingplants.data.ShoppingItem(name = name, quantity = quantity)
                shoppingInventoryDao.insertShoppingItem(item)
            }
        }
    }

    fun toggleShoppingItem(item: com.example.nothingplants.data.ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingInventoryDao.updateShoppingItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun deleteShoppingItem(item: com.example.nothingplants.data.ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingInventoryDao.deleteShoppingItem(item)
        }
    }

    fun clearPurchasedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingInventoryDao.clearPurchasedItems()
        }
    }

    // Inventory Flow ed Azioni
    val allInventoryItems: StateFlow<List<com.example.nothingplants.data.InventoryItem>> = shoppingInventoryDao.getAllInventoryItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addInventoryItem(name: String, levelPercent: Int, description: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank()) {
                val item = com.example.nothingplants.data.InventoryItem(
                    name = name,
                    levelPercent = levelPercent.coerceIn(0, 100),
                    description = if (description.isNullOrBlank()) null else description
                )
                shoppingInventoryDao.insertInventoryItem(item)
            }
        }
    }

    fun updateInventoryItemLevel(item: com.example.nothingplants.data.InventoryItem, newLevel: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val level = newLevel.coerceIn(0, 100)
            shoppingInventoryDao.updateInventoryItem(item.copy(levelPercent = level))
        }
    }

    fun deleteInventoryItem(item: com.example.nothingplants.data.InventoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingInventoryDao.deleteInventoryItem(item)
        }
    }
    
    private val _cloudSyncSummary = MutableStateFlow<SyncSummary?>(null)
    val cloudSyncSummary: StateFlow<SyncSummary?> = _cloudSyncSummary.asStateFlow()

    val autoBackupEnabled: StateFlow<Boolean> = preferencesManager.autoBackupFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val notificationHistory: StateFlow<List<com.example.nothingplants.data.NotificationHistory>> = reminderDao.getAllNotificationHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearNotificationHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteAllNotificationHistory()
        }
    }

    init {
        // Auto-refresh cloud summary on startup if logged in
        viewModelScope.launch {
            try {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(application)
                if (account != null) {
                    refreshCloudSummary(account)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveAutoBackupEnabled(enabled)
            scheduleAutoBackup(enabled)
        }
    }

    fun scheduleAutoBackup(enabled: Boolean) {
        val context = getApplication<Application>()
        val workManager = androidx.work.WorkManager.getInstance(context)
        val backupWorkName = "plants_auto_backup"

        if (!enabled) {
            workManager.cancelUniqueWork(backupWorkName)
            return
        }

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.nothingplants.worker.AutoBackupWorker>(
            24, java.util.concurrent.TimeUnit.HOURS,
            15, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("background_backup")
            .build()

        workManager.enqueueUniquePeriodicWork(
            backupWorkName,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    fun refreshCloudSummary(account: GoogleSignInAccount) {
        viewModelScope.launch {
            syncRepository.getRemoteSyncSummary(account).onSuccess {
                _cloudSyncSummary.value = it
            }
        }
    }

    fun uploadToDrive(account: GoogleSignInAccount, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(syncRepository.upload(account))
        }
    }

    fun downloadFromDrive(account: GoogleSignInAccount, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val res = syncRepository.downloadAndApply(account)
            onResult(res)
            if (res.isSuccess) {
                refreshCloudSummary(account)
            }
        }
    }
    
    sealed interface IdentificationState {
        object Idle : IdentificationState
        object Loading : IdentificationState
        data class Success(
            val species: String,
            val commonName: String,
            val isMix: Boolean,
            val plantCount: Int
        ) : IdentificationState
        data class Error(val message: String) : IdentificationState
    }

    private val _identificationState = MutableStateFlow<IdentificationState>(IdentificationState.Idle)
    val identificationState: StateFlow<IdentificationState> = _identificationState.asStateFlow()

    sealed interface SummaryState {
        object Idle : SummaryState
        object Loading : SummaryState
        data class Success(val summary: String) : SummaryState
        data class Error(val message: String) : SummaryState
    }

    private val _summaryState = MutableStateFlow<SummaryState>(SummaryState.Idle)
    val summaryState: StateFlow<SummaryState> = _summaryState.asStateFlow()

    fun resetSummaryState() {
        _summaryState.value = SummaryState.Idle
    }

    fun generateSummary(species: String, imageUri: Uri?, isMix: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _summaryState.value = SummaryState.Loading
            if (species.isBlank()) {
                _summaryState.value = SummaryState.Error("Specificare la specie o il nome per poter generare la scheda di cura.")
                return@launch
            }
            try {
                val bitmap = imageUri?.let { decodeSampledBitmapFromUri(it) }
                
                val isLocationEnabled = preferencesManager.locationEnabledFlow.firstOrNull() ?: false
                val city = if (isLocationEnabled) preferencesManager.locationCityFlow.firstOrNull() else null
                val lat = if (isLocationEnabled) preferencesManager.locationLatitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                val lon = if (isLocationEnabled) preferencesManager.locationLongitudeFlow.firstOrNull()?.toDoubleOrNull() else null

                val summary = runWithApiKeyFallback { key ->
                    val analyzer = PlantAnalyzer(key)
                    analyzer.generatePlantSummary(species, bitmap, city, lat, lon, isMix)
                }
                if (summary != null) {
                    _summaryState.value = SummaryState.Success(summary)
                } else {
                    _summaryState.value = SummaryState.Error("Gemini non ha restituito una scheda di cura valida.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _summaryState.value = SummaryState.Error(getFriendlyErrorMessage(e))
            }
        }
    }

    val allPlants: StateFlow<List<Plant>> = combine(
        plantDao.getAllPlants(),
        reminderDao.getActiveReminders()
    ) { plants, reminders ->
        val activeWateringReminders = reminders.filter { it.type == "WATERING" && !it.isCompleted }
            .associateBy { it.plantId }
            
        plants.sortedWith { p1, p2 ->
            val r1 = activeWateringReminders[p1.id]
            val r2 = activeWateringReminders[p2.id]
            
            val d1 = r1?.dueDate ?: Long.MAX_VALUE
            val d2 = r2?.dueDate ?: Long.MAX_VALUE
            
            val dateCompare = d1.compareTo(d2)
            if (dateCompare != 0) {
                dateCompare
            } else {
                val name1 = if (p1.name.isNotBlank()) p1.name else p1.species
                val name2 = if (p2.name.isNotBlank()) p2.name else p2.species
                name1.compareTo(name2, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fertilizerSummaryCache: Flow<List<FertilizerSummaryItem>?> = preferencesManager.fertilizerSummaryFlow.map { jsonStr ->
        if (jsonStr.isNullOrBlank()) null
        else {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                val list = mutableListOf<FertilizerSummaryItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val fertType = obj.getString("fertilizer_type")
                    val count = obj.getInt("plants_count")
                    val namesArray = obj.getJSONArray("plant_names")
                    val names = mutableListOf<String>()
                    for (j in 0 until namesArray.length()) {
                        names.add(namesArray.getString(j))
                    }
                    list.add(FertilizerSummaryItem(fertType, count, names))
                }
                list
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    val soilSummaryCache: Flow<List<SoilSummaryItem>?> = preferencesManager.soilSummaryFlow.map { jsonStr ->
        if (jsonStr.isNullOrBlank()) null
        else {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                val list = mutableListOf<SoilSummaryItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val soilType = obj.getString("soil_type")
                    val count = obj.getInt("plants_count")
                    val namesArray = obj.getJSONArray("plant_names")
                    val names = mutableListOf<String>()
                    for (j in 0 until namesArray.length()) {
                        names.add(namesArray.getString(j))
                    }
                    list.add(SoilSummaryItem(soilType, count, names))
                }
                list
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Impostazioni: API Key corrente
    val currentApiKey: StateFlow<String> = preferencesManager.apiKeyFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(key)
        }
    }

    val currentApiKey2: StateFlow<String> = preferencesManager.apiKey2Flow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun saveApiKey2(key: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey2(key)
        }
    }

    // Impostazioni: Geolocalizzazione
    val locationEnabled: StateFlow<Boolean> = preferencesManager.locationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val sortOrder: StateFlow<String> = preferencesManager.sortOrderFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "ROOM")

    fun setSortOrder(order: String) {
        viewModelScope.launch {
            preferencesManager.saveSortOrder(order)
        }
    }

    val locationLatitude: StateFlow<String> = preferencesManager.locationLatitudeFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val locationLongitude: StateFlow<String> = preferencesManager.locationLongitudeFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val locationCity: StateFlow<String> = preferencesManager.locationCityFlow
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    sealed interface LocationLoadingState {
        object Idle : LocationLoadingState
        object Loading : LocationLoadingState
        object Success : LocationLoadingState
        data class Error(val message: String) : LocationLoadingState
    }

    private val _locationLoadingState = MutableStateFlow<LocationLoadingState>(LocationLoadingState.Idle)
    val locationLoadingState: StateFlow<LocationLoadingState> = _locationLoadingState.asStateFlow()

    fun resetLocationLoadingState() {
        _locationLoadingState.value = LocationLoadingState.Idle
    }

    fun setLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveLocationEnabled(enabled)
            if (!enabled) {
                preferencesManager.clearLocationData()
                regenerateAllPlantSummaries(null, null, null)
            }
        }
    }

    fun refreshLocation(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _locationLoadingState.value = LocationLoadingState.Loading
            
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasCoarse && !hasFine) {
                _locationLoadingState.value = LocationLoadingState.Error("Permessi di localizzazione non concessi")
                return@launch
            }
            
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            var location: android.location.Location? = null
            
            try {
                if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                }
                if (location == null && locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            
            if (location == null) {
                try {
                    val provider = when {
                        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) -> android.location.LocationManager.NETWORK_PROVIDER
                        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) -> android.location.LocationManager.GPS_PROVIDER
                        else -> null
                    }
                    
                    if (provider != null) {
                        withContext(Dispatchers.Main) {
                            try {
                                locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                                    override fun onLocationChanged(loc: android.location.Location) {
                                        viewModelScope.launch(Dispatchers.IO) {
                                            processLocation(loc, context)
                                        }
                                    }
                                    override fun onStatusChanged(p: String?, status: Int, extras: android.os.Bundle?) {}
                                    override fun onProviderEnabled(p: String) {}
                                    override fun onProviderDisabled(p: String) {}
                                }, null)
                            } catch (e: SecurityException) {
                                _locationLoadingState.value = LocationLoadingState.Error("Errore permessi di sicurezza")
                            }
                        }
                    } else {
                        _locationLoadingState.value = LocationLoadingState.Error("Nessun provider di localizzazione abilitato")
                    }
                } catch (e: Exception) {
                    _locationLoadingState.value = LocationLoadingState.Error("Errore rilevamento posizione: ${e.message}")
                }
            } else {
                processLocation(location, context)
            }
        }
    }

    private suspend fun processLocation(loc: android.location.Location, context: android.content.Context) {
        val latitude = loc.latitude
        val longitude = loc.longitude
        var cityName = "Posizione Rilevata"
        
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val country = address.countryName ?: ""
                cityName = when {
                    city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                    city.isNotBlank() -> city
                    else -> country.ifBlank { "Lat: ${String.format(java.util.Locale.US, "%.4f", latitude)}, Lon: ${String.format(java.util.Locale.US, "%.4f", longitude)}" }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cityName = "Lat: ${String.format(java.util.Locale.US, "%.4f", latitude)}, Lon: ${String.format(java.util.Locale.US, "%.4f", longitude)}"
        }
        
        preferencesManager.saveLocationData(latitude.toString(), longitude.toString(), cityName)
        _locationLoadingState.value = LocationLoadingState.Success
        
        regenerateAllPlantSummaries(cityName, latitude, longitude)
    }

    private fun regenerateAllPlantSummaries(city: String?, lat: Double?, lon: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            val key1 = preferencesManager.apiKeyFlow.firstOrNull() ?: ""
            val key2 = preferencesManager.apiKey2Flow.firstOrNull() ?: ""
            val plants = plantDao.getAllPlantsSync()
            
            if (key1.isBlank() && key2.isBlank()) {
                // Senza API key non è possibile generare nulla. Rimosso aiSummary per inconsistenza con la nuova localizzazione.
                plants.forEach { plant ->
                    if (plant.aiSummary != null) {
                        plantDao.updatePlant(plant.copy(aiSummary = null))
                    }
                }
                preferencesManager.clearFertilizerSummary()
                return@launch
            }
            
            plants.forEach { plant ->
                try {
                    val bitmap = plant.imageUri?.let { android.graphics.BitmapFactory.decodeFile(it) }
                    val summary = runWithApiKeyFallback { key ->
                        val analyzer = PlantAnalyzer(key)
                        analyzer.generatePlantSummary(
                            species = if (plant.species.isNotBlank()) plant.species else plant.name,
                            image = bitmap,
                            locationCity = city,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                    if (summary != null) {
                        plantDao.updatePlant(plant.copy(aiSummary = summary))
                    } else {
                        // Se fallisce la generazione per Gemini null, cancelliamo l'attuale non più valido
                        plantDao.updatePlant(plant.copy(aiSummary = null))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Se fallisce l'invocazione per errore o eccezione, cancelliamo l'attuale non più valido
                    plantDao.updatePlant(plant.copy(aiSummary = null))
                }
            }
            preferencesManager.clearFertilizerSummary()
        }
    }

    fun getPlant(id: Long): Flow<Plant> = plantDao.getPlantById(id)
    
    fun getActiveReminders(): Flow<List<com.example.nothingplants.data.Reminder>> = reminderDao.getActiveReminders()

    fun getCompletedReminders(): Flow<List<com.example.nothingplants.data.Reminder>> = reminderDao.getCompletedReminders()

    fun markRemindersAsCompleted(plantId: Long, type: String) { viewModelScope.launch(Dispatchers.IO) { reminderDao.markRemindersAsCompleted(plantId, type) } }

    fun toggleReminderStatus(reminder: com.example.nothingplants.data.Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.updateReminderStatus(reminder.id, !reminder.isCompleted)
        }
    }

    fun updateReminderDueDate(reminder: com.example.nothingplants.data.Reminder, newDueDate: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val isCompleted = newDueDate <= now
            val updated = reminder.copy(dueDate = newDueDate, isCompleted = isCompleted)
            reminderDao.updateReminder(updated)
        }
    }

    fun deleteReminder(reminder: com.example.nothingplants.data.Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteReminder(reminder)
        }
    }

    fun applyReminderEdits(
        modifiedDates: Map<Long, Long>,
        deletedIds: List<Long>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Rimuove i promemoria cancellati
            deletedIds.forEach { id ->
                reminderDao.deleteReminderById(id)
            }
            // 2. Aggiorna le date di quelli modificati
            modifiedDates.forEach { (id, newDueDate) ->
                val original = reminderDao.getReminderByIdSync(id)
                if (original != null) {
                    val now = System.currentTimeMillis()
                    val isCompleted = newDueDate <= now
                    reminderDao.updateReminder(original.copy(dueDate = newDueDate, isCompleted = isCompleted))
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deletePlantById(plantId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val plant = plantDao.getPlantByIdSync(plantId)
            if (plant != null) {
                plantDao.deletePlant(plant)
            }
            refreshSoilSummary()
            refreshFertilizerSummary()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun scheduleReminder(plantId: Long, type: String, days: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val dueDate = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000L
            upsertActiveReminder(plantId, type, dueDate)
        }
    }

    private suspend fun upsertActiveReminder(plantId: Long, type: String, dueDate: Long): Long {
        val existing = reminderDao.getActiveReminderForPlantAndTypeSync(plantId, type)
        return if (existing != null) {
            val updated = existing.copy(dueDate = dueDate, isCompleted = false)
            reminderDao.updateReminder(updated)
            existing.id
        } else {
            val newReminder = com.example.nothingplants.data.Reminder(
                plantId = plantId,
                type = type,
                dueDate = dueDate,
                isCompleted = false
            )
            reminderDao.insertReminder(newReminder)
        }
    }

    fun getWateringLogs(plantId: Long): Flow<List<WateringLog>> = plantDao.getWateringLogsForPlant(plantId)

    fun getActiveWateringReminderForPlant(plantId: Long): Flow<com.example.nothingplants.data.Reminder?> {
        return reminderDao.getActiveReminderForPlantAndType(plantId, "WATERING")
    }

    fun getActiveFertilizingReminderForPlant(plantId: Long): Flow<com.example.nothingplants.data.Reminder?> {
        return reminderDao.getActiveReminderForPlantAndType(plantId, "FERTILIZING")
    }

    fun addPlant(
        name: String, 
        species: String, 
        room: String?, 
        adoptionDate: Long, 
        imageUri: Uri?, 
        isAlreadyProcessed: Boolean = false, 
        originalImageUri: Uri? = null,
        aiSummary: String? = null,
        potDiameter: Int? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var localPath: String? = null
            var originalLocalPath: String? = null
            if (imageUri != null) {
                if (isAlreadyProcessed) {
                    localPath = copyImageToInternalStorage(imageUri, "plant_image_")
                } else {
                    val context = getApplication<Application>()
                    val tempPath = java.io.File(context.filesDir, "plant_image_${System.currentTimeMillis()}.jpg").absolutePath
                    val success = try {
                        ImageProcessor.processAndSaveSquareVibrant(context, imageUri, tempPath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                    localPath = if (success) tempPath else copyImageToInternalStorage(imageUri, "plant_image_")
                }
            }
            if (originalImageUri != null) {
                originalLocalPath = copyImageToInternalStorage(originalImageUri, "plant_original_")
            } else if (imageUri != null && !isAlreadyProcessed) {
                originalLocalPath = copyImageToInternalStorage(imageUri, "plant_original_")
            }
            val newPlant = Plant(
                name = name, 
                species = species, 
                imageUri = localPath, 
                originalImageUri = originalLocalPath,
                adoptionDate = adoptionDate, 
                room = if (room.isNullOrBlank()) null else room,
                aiSummary = aiSummary,
                potDiameter = if (potDiameter != null && potDiameter > 0) potDiameter else null
            )
            plantDao.insertPlant(newPlant)
            refreshSoilSummary()
            refreshFertilizerSummary()
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun updatePlantData(
        plantId: Long, 
        name: String, 
        species: String, 
        room: String?, 
        adoptionDate: Long, 
        newImageUri: Uri?, 
        isAlreadyProcessed: Boolean = false, 
        newOriginalImageUri: Uri? = null,
        aiSummary: String? = null,
        potDiameter: Int? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val plant = plantDao.getPlantByIdSync(plantId) ?: return@launch
            var localPath = plant.imageUri
            var originalLocalPath = plant.originalImageUri
            if (newImageUri != null) {
                if (isAlreadyProcessed) {
                    localPath = copyImageToInternalStorage(newImageUri, "plant_image_")
                } else {
                    val context = getApplication<Application>()
                    val tempPath = java.io.File(context.filesDir, "plant_image_${System.currentTimeMillis()}.jpg").absolutePath
                    val success = try {
                        ImageProcessor.processAndSaveSquareVibrant(context, newImageUri, tempPath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                    localPath = if (success) tempPath else copyImageToInternalStorage(newImageUri, "plant_image_")
                }
            }
            if (newOriginalImageUri != null) {
                originalLocalPath = copyImageToInternalStorage(newOriginalImageUri, "plant_original_")
            } else if (newImageUri != null && !isAlreadyProcessed) {
                originalLocalPath = copyImageToInternalStorage(newImageUri, "plant_original_")
            }
            
            val oldDiameter = plant.potDiameter
            val newDiameter = if (potDiameter != null && potDiameter > 0) potDiameter else null
            
            if (newDiameter != null && oldDiameter != newDiameter) {
                // Inseriamo automaticamente un log di rinvaso (REPOTTING) con il nuovo diametro
                val log = WateringLog(
                    logType = "REPOTTING",
                    plantId = plantId,
                    timestamp = System.currentTimeMillis(),
                    newPotDiameter = newDiameter
                )
                plantDao.insertWateringLog(log)
            }
            
            val updatedPlant = plant.copy(
                name = name, 
                species = species, 
                adoptionDate = adoptionDate, 
                imageUri = localPath, 
                originalImageUri = originalLocalPath,
                room = if (room.isNullOrBlank()) null else room,
                aiSummary = aiSummary,
                potDiameter = newDiameter
            )
            plantDao.updatePlant(updatedPlant)
            preferencesManager.clearFertilizerSummary()
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    private fun getFriendlyErrorMessage(e: Exception): String {
        val message = e.message ?: ""
        return when {
            e is org.json.JSONException -> {
                "Risposta dell'AI malformata. Riprova con una foto più chiara."
            }
            e is java.io.IOException || e is java.net.UnknownHostException || e is java.net.ConnectException -> {
                "Nessuna connessione Internet. Controlla la tua rete e riprova."
            }
            message.contains("API_KEY_INVALID", ignoreCase = true) || 
            message.contains("API key not valid", ignoreCase = true) || 
            message.contains("invalid api key", ignoreCase = true) -> {
                "La chiave API (API Key) nelle impostazioni non è valida. Controllala e riprova."
            }
            message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || 
            message.contains("quota", ignoreCase = true) || 
            message.contains("limit exceeded", ignoreCase = true) -> {
                "Limite di richieste superato per Gemini. Riprova tra un minuto."
            }
            message.contains("SAFETY", ignoreCase = true) || 
            message.contains("blocked", ignoreCase = true) -> {
                "L'identificazione è stata bloccata dai filtri di sicurezza di Gemini."
            }
            else -> {
                "Errore durante l'identificazione: ${e.localizedMessage ?: "connessione fallita."}"
            }
        }
    }

    fun identifyPlantSpecies(imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _identificationState.value = IdentificationState.Loading
            try {
                val bitmap = decodeSampledBitmapFromUri(imageUri)
                if (bitmap == null) {
                    _identificationState.value = IdentificationState.Error("Impossibile caricare l'immagine.")
                    return@launch
                }

                val jsonResult = runWithApiKeyFallback { key ->
                    val analyzer = PlantAnalyzer(key)
                    analyzer.identifyPlantSpecies(bitmap)
                }
                if (jsonResult != null) {
                    val json = org.json.JSONObject(jsonResult)
                    val species = json.optString("species", "")
                    val commonName = json.optString("commonName", "")
                    val isMix = json.optBoolean("isMix", false)
                    val plantCount = json.optInt("plantCount", 1)
                    if (species.isNotBlank()) {
                        _identificationState.value = IdentificationState.Success(species, commonName, isMix, plantCount)
                    } else {
                        _identificationState.value = IdentificationState.Error("Nessuna specie identificata nella risposta di Gemini.")
                    }
                } else {
                    _identificationState.value = IdentificationState.Error("Gemini non ha restituito una risposta valida. Riprova o scatta una foto più chiara.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _identificationState.value = IdentificationState.Error(getFriendlyErrorMessage(e))
            }
        }
    }

    fun resetIdentificationState() {
        _identificationState.value = IdentificationState.Idle
    }

    fun generateSummaryForPlant(plantId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val plant = plantDao.getPlantByIdSync(plantId)
            if (plant == null) {
                withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }
            
            try {
                val bitmap = plant.imageUri?.let { android.graphics.BitmapFactory.decodeFile(it) }
                val isLocationEnabled = preferencesManager.locationEnabledFlow.firstOrNull() ?: false
                val city = if (isLocationEnabled) preferencesManager.locationCityFlow.firstOrNull() else null
                val lat = if (isLocationEnabled) preferencesManager.locationLatitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                val lon = if (isLocationEnabled) preferencesManager.locationLongitudeFlow.firstOrNull()?.toDoubleOrNull() else null

                val summary = runWithApiKeyFallback { key ->
                    val analyzer = PlantAnalyzer(key)
                    analyzer.generatePlantSummary(
                        if (plant.species.isNotBlank()) plant.species else plant.name,
                        bitmap,
                        city,
                        lat,
                        lon
                    )
                }
                
                if (summary != null) {
                    val currentPlant = plantDao.getPlantByIdSync(plantId)
                    if (currentPlant != null) {
                        plantDao.updatePlant(currentPlant.copy(aiSummary = summary))
                    }
                    preferencesManager.clearFertilizerSummary()
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    val currentPlant = plantDao.getPlantByIdSync(plantId)
                    if (currentPlant != null && currentPlant.aiSummary != null) {
                        plantDao.updatePlant(currentPlant.copy(aiSummary = null))
                    }
                    preferencesManager.clearFertilizerSummary()
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val currentPlant = plantDao.getPlantByIdSync(plantId)
                if (currentPlant != null && currentPlant.aiSummary != null) {
                    plantDao.updatePlant(currentPlant.copy(aiSummary = null))
                }
                preferencesManager.clearFertilizerSummary()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // Sensori per la luminosità e l'orientamento (bussola)
    private val sensorManager by lazy {
        getApplication<Application>().getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
    }
    private val lightSensor by lazy { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT) }
    private val accelerometer by lazy { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) }
    private val magnetometer by lazy { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) }

    private val _currentLux = MutableStateFlow<Float?>(null)
    val currentLux: StateFlow<Float?> = _currentLux.asStateFlow()

    private val _currentAzimuth = MutableStateFlow<Float?>(null)
    val currentAzimuth: StateFlow<Float?> = _currentAzimuth.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val sensorListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent) {
            when (event.sensor.type) {
                android.hardware.Sensor.TYPE_LIGHT -> {
                    _currentLux.value = event.values[0]
                }
                android.hardware.Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravityValues, 0, 3)
                    hasGravity = true
                }
                android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                    hasGeomagnetic = true
                }
            }

            if (hasGravity && hasGeomagnetic) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if (android.hardware.SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                    val orientation = FloatArray(3)
                    android.hardware.SensorManager.getOrientation(r, orientation)
                    val azimuthRad = orientation[0]
                    var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    if (azimuthDeg < 0) {
                        azimuthDeg += 360f
                    }
                    _currentAzimuth.value = azimuthDeg
                }
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    fun startSensors() {
        _currentLux.value = null
        _currentAzimuth.value = null
        hasGravity = false
        hasGeomagnetic = false

        lightSensor?.let {
            sensorManager.registerListener(sensorListener, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.let {
            sensorManager.registerListener(sensorListener, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopSensors() {
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }

    fun getCompassDirection(azimuth: Float?): String {
        if (azimuth == null) return "Sconosciuta"
        return when (azimuth) {
            in 337.5..360.0, in 0.0..22.5 -> "Nord"
            in 22.5..67.5 -> "Nord-Est"
            in 67.5..112.5 -> "Est"
            in 112.5..157.5 -> "Sud-Est"
            in 157.5..202.5 -> "Sud"
            in 202.5..247.5 -> "Sud-Ovest"
            in 247.5..292.5 -> "Ovest"
            in 292.5..337.5 -> "Nord-Ovest"
            else -> "Sconosciuta"
        }
    }

    fun addGrowthPhoto(
        plantId: Long,
        imageUri: Uri,
        lux: Float?,
        azimuth: Float?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempPath = java.io.File(context.filesDir, "plant_growth_${System.currentTimeMillis()}.jpg").absolutePath
            val success = try {
                ImageProcessor.processAndSaveSquareVibrant(context, imageUri, tempPath)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            val localPath = if (success) tempPath else copyImageToInternalStorage(imageUri)
            val bitmap = localPath?.let { path ->
                decodeSampledBitmapFromUri(Uri.fromFile(java.io.File(path)), reqWidth = 512, reqHeight = 512)
            }
            
            val currentTimestamp = System.currentTimeMillis()
            var score: Int? = null
            var notes: String? = null
            
            val compassDir = if (azimuth != null) getCompassDirection(azimuth) else null
            
            if (bitmap != null) {
                try {
                    val plant = plantDao.getPlantByIdSync(plantId)
                    if (plant != null) {
                        val isLocationEnabled = preferencesManager.locationEnabledFlow.firstOrNull() ?: false
                        val city = if (isLocationEnabled) preferencesManager.locationCityFlow.firstOrNull() else null
                        val lat = if (isLocationEnabled) preferencesManager.locationLatitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                        val lon = if (isLocationEnabled) preferencesManager.locationLongitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                        
                        val result = runWithApiKeyFallback { key ->
                            val analyzer = PlantAnalyzer(key)
                            analyzer.analyzePlant(
                                image = bitmap,
                                plantName = if (plant.name.isNotBlank()) plant.name else plant.species,
                                previousIntervals = emptyList(),
                                logType = "GROWTH",
                                fertilizerRule = null,
                                locationCity = city,
                                latitude = lat,
                                longitude = lon,
                                lux = lux,
                                compassDirection = compassDir,
                                timestamp = currentTimestamp
                            )
                        }
                        if (result != null) {
                            score = result.score
                            notes = result.notes
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val log = WateringLog(
                logType = "GROWTH",
                plantId = plantId,
                timestamp = currentTimestamp,
                imagePath = localPath,
                aiHealthScore = score,
                aiNotes = notes,
                suggestedNextWateringDays = null,
                luxValue = lux,
                compassDirection = compassDir
            )
            plantDao.insertWateringLog(log)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun generateGrowthTrendAnalysis(plantId: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val key1 = preferencesManager.apiKeyFlow.firstOrNull() ?: ""
            val key2 = preferencesManager.apiKey2Flow.firstOrNull() ?: ""
            if (key1.isBlank() && key2.isBlank()) {
                withContext(Dispatchers.Main) {
                    onResult("API Key non configurata nelle impostazioni.")
                }
                return@launch
            }
            
            val plant = plantDao.getPlantByIdSync(plantId)
            if (plant == null) {
                withContext(Dispatchers.Main) {
                    onResult("Pianta non trovata.")
                }
                return@launch
            }
            
            val logs = plantDao.getWateringLogsForPlant(plantId).first()
                .filter { it.logType == "GROWTH" && !it.imagePath.isNullOrBlank() }
                .sortedBy { it.timestamp }
                
            if (logs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult("Nessuna foto disponibile nel diario di crescita per poter effettuare l'analisi.")
                }
                return@launch
            }
            
            val sampledLogs = if (logs.size <= 5) {
                logs
            } else {
                val list = mutableListOf<WateringLog>()
                list.add(logs.first())
                
                val step = (logs.size - 2) / 3.0
                for (i in 1..3) {
                    val index = Math.round(i * step).toInt()
                    if (index > 0 && index < logs.size - 1 && !list.contains(logs[index])) {
                        list.add(logs[index])
                    }
                }
                
                if (!list.contains(logs.last())) {
                    list.add(logs.last())
                }
                list.sortedBy { it.timestamp }
            }
            
            val bitmaps = mutableListOf<Bitmap>()
            val dates = mutableListOf<String>()
            
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("it", "IT"))
            
            sampledLogs.forEach { log ->
                val bitmap = log.imagePath?.let { path ->
                    try {
                        decodeSampledBitmapFromUri(Uri.fromFile(java.io.File(path)), reqWidth = 512, reqHeight = 512)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    bitmaps.add(bitmap)
                    dates.add(formatter.format(java.util.Date(log.timestamp)))
                }
            }
            
            if (bitmaps.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult("Impossibile caricare le foto del diario.")
                }
                return@launch
            }
            
            try {
                val report = runWithApiKeyFallback { key ->
                    val analyzer = PlantAnalyzer(key)
                    analyzer.analyzeGrowthTrend(
                        species = if (plant.species.isNotBlank()) plant.species else plant.name,
                        images = bitmaps,
                        dates = dates
                    )
                }
                withContext(Dispatchers.Main) {
                    onResult(report)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult("Errore durante l'analisi dell'andamento: ${e.localizedMessage}")
                }
            }
        }
    }

    fun addCareLogWithPhoto(plant: Plant, imageUri: Uri, logType: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempPath = java.io.File(context.filesDir, "plant_water_${System.currentTimeMillis()}.jpg").absolutePath
            val success = try {
                ImageProcessor.processAndSaveSquareVibrant(context, imageUri, tempPath)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            val localPath = if (success) tempPath else copyImageToInternalStorage(imageUri)
            
            val bitmap = localPath?.let { BitmapFactory.decodeFile(it) }
            
            var score: Int? = null
            var notes: String? = null
            var daysToNext: Int? = null
            
            if (bitmap != null) {
                try {
                    // Recuperiamo gli intervalli precedenti se annaffiatura
                    val logs = plantDao.getWateringLogsForPlant(plant.id).first()
                    val intervals = if (logType == "WATERING") calculateIntervals(logs) else emptyList()
                    
                    var fertilizerRule: String? = null
                    if (logType == "FERTILIZING" && plant.aiSummary != null) {
                        try {
                            val json = org.json.JSONObject(plant.aiSummary)
                            fertilizerRule = json.optString("fertilizer")
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    
                    val isLocationEnabled = preferencesManager.locationEnabledFlow.firstOrNull() ?: false
                    val city = if (isLocationEnabled) preferencesManager.locationCityFlow.firstOrNull() else null
                    val lat = if (isLocationEnabled) preferencesManager.locationLatitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                    val lon = if (isLocationEnabled) preferencesManager.locationLongitudeFlow.firstOrNull()?.toDoubleOrNull() else null

                    val result = runWithApiKeyFallback { key ->
                        val analyzer = PlantAnalyzer(key)
                        analyzer.analyzePlant(bitmap, plant.name, intervals, logType, fertilizerRule, city, lat, lon)
                    }
                    
                    if (result != null) {
                        score = result.score
                        notes = result.notes
                        daysToNext = result.daysToNext
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Calcolo stima finale (Gemini o Fallback basato su report di cura)
            val finalDaysToNext = if (daysToNext != null && daysToNext > 0) {
                daysToNext
            } else {
                getIdealDaysForPlantCare(plant, logType, if (logType == "WATERING") 7 else 30)
            }

            // Programmiamo il promemoria
            scheduleReminder(plant.id, logType, finalDaysToNext)

            // Inseriamo il log
            val log = WateringLog(
                logType = logType,
                plantId = plant.id,
                timestamp = System.currentTimeMillis(),
                imagePath = localPath,
                aiHealthScore = score,
                aiNotes = notes,
                suggestedNextWateringDays = finalDaysToNext
            )
            plantDao.insertWateringLog(log)
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    fun addCareLogWithoutPhoto(plant: Plant, logType: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultDays = getIdealDaysForPlantCare(plant, logType, if (logType == "WATERING") 7 else 30)
            
            // Aggiorniamo o inseriamo il promemoria attivo
            val dueDate = System.currentTimeMillis() + defaultDays * 24L * 60 * 60 * 1000L
            val reminderId = upsertActiveReminder(plant.id, logType, dueDate)
            
            // Inseriamo il log iniziale immediatamente
            val log = WateringLog(
                logType = logType,
                plantId = plant.id,
                timestamp = System.currentTimeMillis(),
                imagePath = null,
                aiHealthScore = null,
                aiNotes = null,
                suggestedNextWateringDays = defaultDays
            )
            val logId = plantDao.insertWateringLog(log)
            
            // Comunichiamo subito il successo alla UI
            withContext(Dispatchers.Main) { onSuccess() }
            
            // Eseguiamo la chiamata in background a Gemini SOLO se non c'è un aiSummary (report di cura non presente)
            if (plant.aiSummary.isNullOrBlank()) {
                try {
                    val logs = plantDao.getWateringLogsForPlant(plant.id).first()
                    val intervals = if (logType == "WATERING") calculateIntervals(logs) else emptyList()
                    val isLocationEnabled = preferencesManager.locationEnabledFlow.firstOrNull() ?: false
                    val city = if (isLocationEnabled) preferencesManager.locationCityFlow.firstOrNull() else null
                    val lat = if (isLocationEnabled) preferencesManager.locationLatitudeFlow.firstOrNull()?.toDoubleOrNull() else null
                    val lon = if (isLocationEnabled) preferencesManager.locationLongitudeFlow.firstOrNull()?.toDoubleOrNull() else null

                    val result = runWithApiKeyFallback { key ->
                        val analyzer = PlantAnalyzer(key)
                        analyzer.analyzePlant(null, plant.name, intervals, logType, null, city, lat, lon)
                    }
                    
                    if (result != null && result.daysToNext > 0) {
                        val daysToNext = result.daysToNext
                        val newDueDate = System.currentTimeMillis() + daysToNext * 24L * 60 * 60 * 1000L
                        upsertActiveReminder(plant.id, logType, newDueDate)
                        
                        val savedLog = plantDao.getWateringLogByIdSync(logId)
                        if (savedLog != null) {
                            plantDao.updateWateringLog(savedLog.copy(suggestedNextWateringDays = daysToNext))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteWateringLog(log: WateringLog) {
        viewModelScope.launch(Dispatchers.IO) {
            plantDao.deleteWateringLog(log)
        }
    }

    fun updateWateringLogTimestamp(log: WateringLog, newTimestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            plantDao.updateWateringLog(log.copy(timestamp = newTimestamp))
        }
    }

    private fun calculateIntervals(logs: List<WateringLog>): List<Int> {
        if (logs.size < 2) return emptyList()
        val sortedLogs = logs.sortedBy { it.timestamp }
        val intervals = mutableListOf<Int>()
        for (i in 1 until sortedLogs.size) {
            val diffMs = sortedLogs[i].timestamp - sortedLogs[i - 1].timestamp
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays > 0) {
                intervals.add(diffDays)
            }
        }
        return intervals
    }

    private fun copyImageToInternalStorage(uri: Uri, prefix: String = "plant_image_"): String? {
        val context = getApplication<Application>()
        return try {
            val file = File(context.filesDir, "${prefix}${System.currentTimeMillis()}.jpg")
            if (uri.scheme == "file") {
                val srcFile = File(uri.path ?: throw Exception("Path nullo per il file URI"))
                srcFile.copyTo(file, overwrite = true)
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Impossibile aprire l'input stream")
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int = 1024, reqHeight: Int = 1024): Bitmap? {
        val context = getApplication<Application>()
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            if (uri.scheme == "file") {
                val path = uri.path ?: return null
                android.graphics.BitmapFactory.decodeFile(path, options)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input, null, options)
                }
            }
            
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            val decoded = if (uri.scheme == "file") {
                val path = uri.path ?: return null
                android.graphics.BitmapFactory.decodeFile(path, options)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input, null, options)
                }
            }
            
            if (decoded != null) {
                val width = decoded.width
                val height = decoded.height
                val maxDim = maxOf(width, height)
                if (maxDim > reqWidth) {
                    val scale = reqWidth.toFloat() / maxDim
                    Bitmap.createScaledBitmap(decoded, (width * scale).toInt(), (height * scale).toInt(), true)
                } else {
                    decoded
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getIdealDaysForPlantCare(plant: Plant, logType: String, defaultDays: Int): Int {
        val summary = plant.aiSummary ?: return defaultDays
        if (summary.isBlank()) return defaultDays
        
        try {
            val json = org.json.JSONObject(summary)
            val key = if (logType == "WATERING") "water_interval_days" else "fertilizer_interval_days"
            if (json.has(key)) {
                val days = json.getInt(key)
                if (days > 0) return days
            }
            
            val textKey = if (logType == "WATERING") "water" else "fertilizer"
            val text = json.optString(textKey, "")
            if (text.isNotBlank()) {
                val analyzer = PlantAnalyzer("")
                return analyzer.extractDaysFromText(text, logType == "WATERING")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultDays
    }

    fun verifyAllReminders(onResult: (List<ReminderProposal>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = plantDao.getAllPlants().first()
            val proposals = mutableListOf<ReminderProposal>()

            plants.forEach { plant ->
                val logs = plantDao.getWateringLogsForPlant(plant.id).first()
                val plantName = if (plant.name.isNotBlank()) plant.name else plant.species
                
                listOf("WATERING", "FERTILIZING").forEach { type ->
                    val lastLog = logs.firstOrNull { it.logType == type }
                    val lastEventTime = lastLog?.timestamp ?: plant.adoptionDate
                    
                    val activeReminder = reminderDao.getActiveReminderForPlantAndTypeSync(plant.id, type)
                    
                    if (plant.aiSummary.isNullOrBlank()) {
                        proposals.add(
                            ReminderProposal(
                                plantId = plant.id,
                                plantName = plantName,
                                type = type,
                                currentDueDate = activeReminder?.dueDate,
                                proposedDueDate = 0L,
                                daysInterval = 0,
                                textSource = "",
                                isDelete = false,
                                isError = true,
                                errorText = "Impossibile generare promemoria per la pianta ${plantName} (Report AI non presente)"
                            )
                        )
                        return@forEach
                    }
                    
                    var days = 0
                    var textSource = ""
                    
                    try {
                        val json = org.json.JSONObject(plant.aiSummary)
                        textSource = json.optString(if (type == "WATERING") "water" else "fertilizer", "")
                        
                        val key = if (type == "WATERING") "water_interval_days" else "fertilizer_interval_days"
                        if (json.has(key)) {
                            days = json.getInt(key)
                        } else {
                            val analyzer = com.example.nothingplants.ai.PlantAnalyzer("")
                            days = analyzer.extractDaysFromText(textSource, type == "WATERING")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (type == "FERTILIZING" && days == 0) {
                        if (activeReminder != null) {
                            proposals.add(
                                ReminderProposal(
                                    plantId = plant.id,
                                    plantName = plantName,
                                    type = type,
                                    currentDueDate = activeReminder.dueDate,
                                    proposedDueDate = 0L,
                                    daysInterval = 0,
                                    textSource = textSource,
                                    isDelete = true
                                )
                            )
                        }
                    } else if (days <= 0) {
                        proposals.add(
                            ReminderProposal(
                                plantId = plant.id,
                                plantName = plantName,
                                type = type,
                                currentDueDate = activeReminder?.dueDate,
                                proposedDueDate = 0L,
                                daysInterval = 0,
                                textSource = textSource,
                                isError = true,
                                errorText = "Impossibile generare promemoria per la pianta ${plantName} (Nessun riferimento temporale rilevato)"
                            )
                        )
                    } else {
                        val proposedDueDate = lastEventTime + days * 24L * 60 * 60 * 1000L
                        val calendarProposed = java.util.Calendar.getInstance().apply {
                            timeInMillis = proposedDueDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        
                        if (activeReminder == null) {
                            proposals.add(
                                ReminderProposal(
                                    plantId = plant.id,
                                    plantName = plantName,
                                    type = type,
                                    currentDueDate = null,
                                    proposedDueDate = proposedDueDate,
                                    daysInterval = days,
                                    textSource = textSource,
                                    isDelete = false
                                )
                            )
                        } else {
                            val calendarActive = java.util.Calendar.getInstance().apply {
                                timeInMillis = activeReminder.dueDate
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            
                            val diffDays = Math.abs(calendarProposed.timeInMillis - calendarActive.timeInMillis) / (24L * 60 * 60 * 1000)
                            if (diffDays >= 1) {
                                proposals.add(
                                    ReminderProposal(
                                        plantId = plant.id,
                                        plantName = plantName,
                                        type = type,
                                        currentDueDate = activeReminder.dueDate,
                                        proposedDueDate = proposedDueDate,
                                        daysInterval = days,
                                        textSource = textSource,
                                        isDelete = false
                                    )
                                )
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(proposals)
            }
        }
    }

    fun applyReminderProposals(proposals: List<ReminderProposal>, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            proposals.forEach { proposal ->
                if (proposal.isError) return@forEach // Salta i casi di errore
                
                if (proposal.isDelete) {
                    val active = reminderDao.getActiveReminderForPlantAndTypeSync(proposal.plantId, proposal.type)
                    if (active != null) {
                        reminderDao.deleteReminder(active)
                    }
                } else {
                    upsertActiveReminder(proposal.plantId, proposal.type, proposal.proposedDueDate)
                }

                val plant = plantDao.getPlantByIdSync(proposal.plantId)
                if (plant != null && !plant.aiSummary.isNullOrBlank()) {
                    try {
                        val json = org.json.JSONObject(plant.aiSummary)
                        val key = if (proposal.type == "WATERING") "water_interval_days" else "fertilizer_interval_days"
                        json.put(key, proposal.daysInterval)
                        plantDao.updatePlant(plant.copy(aiSummary = json.toString()))
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun getFertilizerSummary(onResult: (List<FertilizerSummaryItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = plantDao.getAllPlants().first()
            
            // Se c'è l'API key, usiamo Gemini 3.5 Flash per raggruppare i fertilizzanti in categorie omogenee e pulite
            val items = try {
                runWithApiKeyFallback { key ->
                    val plantsData = org.json.JSONArray()
                    plants.forEach { plant ->
                        var fertText = "Nessun report generato"
                        if (!plant.aiSummary.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(plant.aiSummary)
                                fertText = json.optString("fertilizer", "Nessun fertilizzante specificato")
                            } catch(e: Exception) {}
                        }
                        val obj = org.json.JSONObject().apply {
                            put("id", plant.id)
                            put("name", if (plant.name.isNotBlank()) plant.name else plant.species)
                            put("fertilizer_text", fertText)
                        }
                        plantsData.put(obj)
                    }
                    
                    val prompt = """
                        Sei un botanico esperto. Ti viene fornita una lista di piante in formato JSON, ciascuna con il testo descrittivo del concime necessario (fertilizer_text).
                        Il tuo compito è raggruppare queste piante in macro-categorie omogenee di concime (es. "Concime liquido per piante verdi", "Concime specifico per succulente/cactus", "Nessun concime richiesto", "Concime universale", ecc.).
                        Le categorie devono essere scritte in italiano, pulite e concise.
                        
                        Restituisci ESATTAMENTE e SOLO un array JSON nel seguente formato (nessun blocco markdown o altro testo, nessun ```json):
                        [
                          {
                            "fertilizer_type": "Nome della macro-categoria di concime",
                            "plant_ids": [1, 2, 3]
                          }
                        ]
                        
                        Dati delle piante:
                        ${plantsData.toString()}
                    """.trimIndent()
                    
                    val analyzer = com.example.nothingplants.ai.PlantAnalyzer(key)
                    val responseText = analyzer.generativeModel.generateContent(prompt).text ?: ""
                    val startIndex = responseText.indexOf('[')
                    val endIndex = responseText.lastIndexOf(']')
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        val jsonText = responseText.substring(startIndex, endIndex + 1).trim()
                        val jsonArray = org.json.JSONArray(jsonText)
                        val summaryList = mutableListOf<FertilizerSummaryItem>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val fertType = obj.getString("fertilizer_type")
                            val idsArray = obj.getJSONArray("plant_ids")
                            val plantNames = mutableListOf<String>()
                            for (j in 0 until idsArray.length()) {
                                val id = idsArray.getLong(j)
                                val p = plants.firstOrNull { it.id == id }
                                if (p != null) {
                                    plantNames.add(if (p.name.isNotBlank()) p.name else p.species)
                                }
                            }
                            if (plantNames.isNotEmpty()) {
                                summaryList.add(
                                    FertilizerSummaryItem(
                                        fertilizerType = fertType.uppercase(),
                                        plantsCount = plantNames.size,
                                        plantNames = plantNames
                                    )
                                )
                            }
                        }
                        summaryList
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            
            // Fallback locale se non c'è l'API key o la chiamata fallisce
            val finalItems = items ?: run {
                val groups = mutableMapOf<String, MutableList<String>>()
                plants.forEach { plant ->
                    val name = if (plant.name.isNotBlank()) plant.name else plant.species
                    var fertText = "Nessun report generato"
                    if (!plant.aiSummary.isNullOrBlank()) {
                        try {
                            val json = org.json.JSONObject(plant.aiSummary)
                            fertText = json.optString("fertilizer", "").lowercase()
                        } catch(e: Exception) {}
                    }
                    
                    val category = when {
                        fertText.contains("piante verdi") || fertText.contains("foglia") -> "CONCIME LIQUIDO PER PIANTE VERDI"
                        fertText.contains("succulent") || fertText.contains("cactus") || fertText.contains("grasse") -> "CONCIME PER PIANTE SUCCULENTE / CACTUS"
                        fertText.contains("fiorit") || fertText.contains("fiori") -> "CONCIME PER PIANTE DA FIORE"
                        fertText.contains("organico") || fertText.contains("lenta cessione") || fertText.contains("granulare") -> "CONCIME GRANULARE A LENTA CESSIONE"
                        fertText.contains("nessun") || fertText.contains("non necessita") || fertText.contains("sospendere") && !fertText.contains("ogni") -> "NESSUN CONCIME RICHIESTO"
                        fertText.contains("report generato") || fertText.contains("assent") || fertText.isBlank() -> "DATI DI CURA AI ASSENTI"
                        else -> "CONCIME UNIVERSALE BILANCIATO"
                    }
                    groups.getOrPut(category) { mutableListOf() }.add(name)
                }
                groups.map { (cat, list) ->
                    FertilizerSummaryItem(
                        fertilizerType = cat,
                        plantsCount = list.size,
                        plantNames = list
                    )
                }
            }
            
            withContext(Dispatchers.Main) {
                onResult(finalItems.sortedByDescending { it.plantsCount })
            }
        }
    }

    fun getSoilSummary(onResult: (List<SoilSummaryItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val plants = plantDao.getAllPlants().first()
            
            // Se c'è l'API key, usiamo Gemini 3.5 Flash per raggruppare i terricci in categorie omogenee e pulite
            val items = try {
                runWithApiKeyFallback { key ->
                    val plantsData = org.json.JSONArray()
                    plants.forEach { plant ->
                        var soilText = "Nessun report generato"
                        if (!plant.aiSummary.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(plant.aiSummary)
                                soilText = json.optString("soil", "Nessun terriccio specificato")
                            } catch(e: Exception) {}
                        }
                        val obj = org.json.JSONObject().apply {
                            put("id", plant.id)
                            put("name", if (plant.name.isNotBlank()) plant.name else plant.species)
                            put("soil_text", soilText)
                        }
                        plantsData.put(obj)
                    }
                    
                    val prompt = """
                        Sei un botanico esperto. Ti viene fornita una lista di piante in formato JSON, ciascuna con il testo descrittivo del terriccio necessario (soil_text).
                        Il tuo compito è raggruppare queste piante in macro-categorie omogenee di terriccio (es. "Terriccio per piante succulente e cactus", "Terriccio universale ben drenato", "Terriccio acido per acidofile", ecc.).
                        Le categorie devono essere scritte in italiano, pulite e concise.
                        
                        Restituisci ESATTAMENTE e SOLO un array JSON nel seguente formato (nessun blocco markdown o altro testo, nessun ```json):
                        [
                          {
                            "soil_type": "Nome della macro-categoria di terriccio",
                            "plant_ids": [1, 2, 3]
                          }
                        ]
                        
                        Dati delle piante:
                        ${plantsData.toString()}
                    """.trimIndent()
                    
                    val analyzer = com.example.nothingplants.ai.PlantAnalyzer(key)
                    val responseText = analyzer.generativeModel.generateContent(prompt).text ?: ""
                    val startIndex = responseText.indexOf('[')
                    val endIndex = responseText.lastIndexOf(']')
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        val jsonText = responseText.substring(startIndex, endIndex + 1).trim()
                        val jsonArray = org.json.JSONArray(jsonText)
                        val summaryList = mutableListOf<SoilSummaryItem>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val soilType = obj.getString("soil_type")
                            val idsArray = obj.getJSONArray("plant_ids")
                            val plantNames = mutableListOf<String>()
                            for (j in 0 until idsArray.length()) {
                                val id = idsArray.getLong(j)
                                val p = plants.firstOrNull { it.id == id }
                                if (p != null) {
                                    plantNames.add(if (p.name.isNotBlank()) p.name else p.species)
                                }
                            }
                            if (plantNames.isNotEmpty()) {
                                summaryList.add(
                                    SoilSummaryItem(
                                        soilType = soilType.uppercase(),
                                        plantsCount = plantNames.size,
                                        plantNames = plantNames
                                    )
                                )
                            }
                        }
                        summaryList
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            
            // Fallback locale se non c'è l'API key o la chiamata fallisce
            val finalItems = items ?: run {
                val groups = mutableMapOf<String, MutableList<String>>()
                plants.forEach { plant ->
                    val name = if (plant.name.isNotBlank()) plant.name else plant.species
                    var soilText = "Nessun report generato"
                    if (!plant.aiSummary.isNullOrBlank()) {
                        try {
                            val json = org.json.JSONObject(plant.aiSummary)
                            soilText = json.optString("soil", "").lowercase()
                        } catch(e: Exception) {}
                    }
                    
                    val category = when {
                        soilText.contains("succulent") || soilText.contains("cactus") || soilText.contains("sabbia") || soilText.contains("grasse") -> "TERRICCIO PER PIANTE SUCCULENTE / CACTUS"
                        soilText.contains("acidofil") || soilText.contains("azalea") || soilText.contains("ortensia") -> "TERRICCIO PER PIANTE ACIDOFILE"
                        soilText.contains("orchidee") || soilText.contains("corteccia") -> "SUBSTRATO SPECIFICO PER ORCHIDEE"
                        soilText.contains("torba") || soilText.contains("universale") || soilText.contains("perlite") -> "TERRICCIO UNIVERSALE DRENANTE CON PERLITE"
                        soilText.contains("report generato") || soilText.contains("assent") || soilText.isBlank() -> "DATI DI CURA AI ASSENTI"
                        else -> "TERRICCIO UNIVERSALE BEN DRENATO"
                    }
                    groups.getOrPut(category) { mutableListOf() }.add(name)
                }
                groups.map { (cat, list) ->
                    SoilSummaryItem(
                        soilType = cat,
                        plantsCount = list.size,
                        plantNames = list
                    )
                }
            }
            
            withContext(Dispatchers.Main) {
                onResult(finalItems.sortedByDescending { it.plantsCount })
            }
        }
    }

    fun refreshSoilSummary(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            getSoilSummary { result ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val jsonArray = org.json.JSONArray()
                        result.forEach { item ->
                            val obj = org.json.JSONObject().apply {
                                put("soil_type", item.soilType)
                                put("plants_count", item.plantsCount)
                                val namesArray = org.json.JSONArray()
                                item.plantNames.forEach { namesArray.put(it) }
                                put("plant_names", namesArray)
                            }
                            jsonArray.put(obj)
                        }
                        preferencesManager.saveSoilSummary(jsonArray.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        }
    }

    fun refreshFertilizerSummary(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            getFertilizerSummary { result ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val jsonArray = org.json.JSONArray()
                        result.forEach { item ->
                            val obj = org.json.JSONObject().apply {
                                put("fertilizer_type", item.fertilizerType)
                                put("plants_count", item.plantsCount)
                                val namesArray = org.json.JSONArray()
                                item.plantNames.forEach { namesArray.put(it) }
                                put("plant_names", namesArray)
                            }
                            jsonArray.put(obj)
                        }
                        preferencesManager.saveFertilizerSummary(jsonArray.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        }
    }

    sealed interface PhotoHealthState {
        object Idle : PhotoHealthState
        object Loading : PhotoHealthState
        data class Success(val report: String) : PhotoHealthState
        data class Error(val message: String) : PhotoHealthState
    }

    private val _photoHealthState = MutableStateFlow<PhotoHealthState>(PhotoHealthState.Idle)
    val photoHealthState: StateFlow<PhotoHealthState> = _photoHealthState.asStateFlow()

    fun resetPhotoHealthState() {
        _photoHealthState.value = PhotoHealthState.Idle
    }

    fun analyzePhotoHealth(imagePathsOrUris: List<String>, speciesName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _photoHealthState.value = PhotoHealthState.Loading
            try {
                val bitmaps = imagePathsOrUris.mapNotNull { path ->
                    val uri = Uri.parse(path)
                    val finalUri = if (uri.scheme == null) {
                        Uri.fromFile(File(path))
                    } else {
                        uri
                    }
                    decodeSampledBitmapFromUri(finalUri)
                }
                if (bitmaps.isEmpty()) {
                    _photoHealthState.value = PhotoHealthState.Error("Impossibile caricare le immagini selezionate.")
                    return@launch
                }
                val report = runWithApiKeyFallback { key ->
                    val analyzer = PlantAnalyzer(key)
                    analyzer.analyzePhotoHealth(bitmaps, speciesName)
                }
                if (report != null) {
                    _photoHealthState.value = PhotoHealthState.Success(report)
                } else {
                    _photoHealthState.value = PhotoHealthState.Error("L'analisi AI della salute non ha restituito alcun risultato.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _photoHealthState.value = PhotoHealthState.Error(getFriendlyErrorMessage(e))
            }
        }
    }

    fun associateHealthCheckToPlant(
        plantId: Long,
        imageUris: List<Uri>,
        report: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            imageUris.forEachIndexed { index, imageUri ->
                val tempPath = java.io.File(context.filesDir, "plant_growth_${System.currentTimeMillis()}_$index.jpg").absolutePath
                val success = try {
                    ImageProcessor.processAndSaveSquareVibrant(context, imageUri, tempPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                val localPath = if (success) tempPath else copyImageToInternalStorage(imageUri, "plant_growth_")
                
                // Mettiamo il report come nota in ciascun log, in modo che sia accessibile da qualunque foto associata a questa sessione
                val log = WateringLog(
                    logType = "GROWTH",
                    plantId = plantId,
                    timestamp = System.currentTimeMillis() + index, // piccolo offset per differenziare l'ordinamento
                    imagePath = localPath,
                    aiHealthScore = null,
                    aiNotes = report
                )
                plantDao.insertWateringLog(log)
            }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }

    private suspend fun <T> runWithApiKeyFallback(block: suspend (apiKey: String) -> T): T {
        val key1 = preferencesManager.apiKeyFlow.firstOrNull() ?: ""
        val key2 = preferencesManager.apiKey2Flow.firstOrNull() ?: ""
        
        if (key1.isBlank() && key2.isBlank()) {
            throw Exception("API Key non configurata nelle impostazioni.")
        }
        
        if (key1.isNotBlank()) {
            try {
                return block(key1.trim())
            } catch (e: Exception) {
                e.printStackTrace()
                // Se la chiave primaria fallisce per qualsiasi motivo (limiti, rete o chiave non valida)
                // e abbiamo configurato una chiave secondaria, tentiamo con quest'ultima.
                if (key2.isBlank()) {
                    throw e
                }
            }
        }
        
        return block(key2.trim())
    }
}

data class ReminderProposal(
    val plantId: Long,
    val plantName: String,
    val type: String, // "WATERING" or "FERTILIZING"
    val currentDueDate: Long?,
    val proposedDueDate: Long,
    val daysInterval: Int,
    val textSource: String,
    val isDelete: Boolean = false,
    val isError: Boolean = false,
    val errorText: String? = null
)

data class FertilizerSummaryItem(
    val fertilizerType: String,
    val plantsCount: Int,
    val plantNames: List<String>
)

data class SoilSummaryItem(
    val soilType: String,
    val plantsCount: Int,
    val plantNames: List<String>
)



