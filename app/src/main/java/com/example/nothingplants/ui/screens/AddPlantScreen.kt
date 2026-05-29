package com.example.nothingplants.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nothingplants.ui.CameraUtils
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.provider.MediaStore
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    plantId: Long? = null,
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    var plantName by remember { mutableStateOf("") }
    var plantSpecies by remember { mutableStateOf("") }
    var plantRoom by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    var existingOriginalImageUrl by remember { mutableStateOf<String?>(null) }
    var isNewPhoto by remember { mutableStateOf(false) }
    var adoptionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var processedImageFile by remember { mutableStateOf<java.io.File?>(null) }
    var croppedUri by remember { mutableStateOf<Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var aiSummaryState by remember { mutableStateOf<String?>(null) }
    var potDiameterStr by remember { mutableStateOf("") }
    
    var aiDetectedIsMix by remember { mutableStateOf(false) }
    var aiDetectedPlantCount by remember { mutableStateOf(1) }
    var userSaysIsMix by remember { mutableStateOf(false) }
    var userPlantCountInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val identificationState by viewModel.identificationState.collectAsState()
    val currentApiKey by viewModel.currentApiKey.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()

    LaunchedEffect(imageUri, processedImageFile) {
        viewModel.resetIdentificationState()
        viewModel.resetSummaryState()
        aiDetectedIsMix = false
        aiDetectedPlantCount = 1
        userSaysIsMix = false
        userPlantCountInput = ""
    }

    LaunchedEffect(summaryState) {
        if (summaryState is PlantViewModel.SummaryState.Success) {
            aiSummaryState = (summaryState as PlantViewModel.SummaryState.Success).summary
            viewModel.resetSummaryState()
        }
    }

    val originalBitmap = remember(imageUri) {
        val uri = imageUri
        if (uri != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    LaunchedEffect(originalBitmap) {
        if (originalBitmap != null) {
            showCropDialog = true
        }
    }

    LaunchedEffect(originalBitmap, imageUri) {
        if (imageUri != null && originalBitmap == null) {
            errorMessage = "Impossibile caricare la foto originale. Assicurati che il file non sia danneggiato e riprova."
            showErrorDialog = true
            imageUri = null
        }
    }

    LaunchedEffect(croppedUri) {
        val uri = croppedUri
        if (uri != null) {
            processedImageFile = java.io.File(uri.path ?: "")
        } else {
            processedImageFile = null
        }
    }

    LaunchedEffect(plantId) {
        if (plantId != null) {
            val plant = viewModel.getPlant(plantId).firstOrNull()
            if (plant != null) {
                plantName = plant.name
                plantSpecies = plant.species
                plantRoom = plant.room ?: ""
                existingImageUrl = plant.imageUri
                existingOriginalImageUrl = plant.originalImageUri
                adoptionDate = plant.adoptionDate
                aiSummaryState = plant.aiSummary
                potDiameterStr = plant.potDiameter?.toString() ?: ""
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempUri
            isNewPhoto = true
        }
    }

    val isEditMode = plantId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) "Modifica Pianta" else "Nuova Pianta", 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina pianta",
                                tint = NothingRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = plantName,
                onValueChange = { plantName = it },
                label = { Text("Nome della pianta", fontFamily = NothingFontFamily) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    focusedLabelColor = NothingRed,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            
            OutlinedTextField(
                value = plantSpecies,
                onValueChange = { plantSpecies = it },
                label = { Text("Specie (opzionale)", fontFamily = NothingFontFamily) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    focusedLabelColor = NothingRed,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            OutlinedTextField(
                value = plantRoom,
                onValueChange = { plantRoom = it },
                label = { Text("Posizione / Stanza (opzionale)", fontFamily = NothingFontFamily) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    focusedLabelColor = NothingRed,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            OutlinedTextField(
                value = potDiameterStr,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        potDiameterStr = newValue
                    }
                },
                label = { Text("Diametro del vaso (cm) (opzionale)", fontFamily = NothingFontFamily) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    focusedLabelColor = NothingRed,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            val adoptionStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(adoptionDate))
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = adoptionStr,
                    onValueChange = {},
                    label = { Text("Data di adozione", fontFamily = NothingFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onBackground,
                        disabledBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = adoptionDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                adoptionDate = it
                            }
                            showDatePicker = false
                        }) {
                            Text("OK", fontFamily = NothingFontFamily, color = NothingRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("ANNULLA", fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            // Selettore Fotocamera
            val hasImage = processedImageFile != null || imageUri != null || existingImageUrl != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .then(
                        if (!hasImage) {
                            Modifier.clickable { 
                                val uri = CameraUtils.createTempImageUri(context)
                                tempUri = uri
                                cameraLauncher.launch(uri)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingImage) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NothingRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ELABORAZIONE FOTO...",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else if (hasImage) {
                    AsyncImage(
                        model = processedImageFile ?: imageUri ?: existingImageUrl,
                        contentDescription = "Plant image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Icona di modifica sovrapposta in basso a destra (Cambia foto)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                val uri = CameraUtils.createTempImageUri(context)
                                tempUri = uri
                                cameraLauncher.launch(uri)
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Cambia foto",
                            tint = Color.White
                        )
                    }

                    // Icona di ritaglio/regolazione in basso a sinistra (Ritaglia foto)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                val currentPhotoUri = when {
                                    imageUri != null -> imageUri
                                    existingOriginalImageUrl != null -> {
                                        if (existingOriginalImageUrl!!.startsWith("content://") || existingOriginalImageUrl!!.startsWith("file://")) {
                                            Uri.parse(existingOriginalImageUrl)
                                        } else {
                                            Uri.fromFile(java.io.File(existingOriginalImageUrl!!))
                                        }
                                    }
                                    processedImageFile != null -> Uri.fromFile(processedImageFile!!)
                                    existingImageUrl != null -> {
                                        if (existingImageUrl!!.startsWith("content://") || existingImageUrl!!.startsWith("file://")) {
                                            Uri.parse(existingImageUrl)
                                        } else {
                                            Uri.fromFile(java.io.File(existingImageUrl!!))
                                        }
                                    }
                                    else -> null
                                }
                                if (currentPhotoUri != null) {
                                    if (imageUri == currentPhotoUri && originalBitmap != null) {
                                        showCropDialog = true
                                    } else {
                                        imageUri = currentPhotoUri
                                    }
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Ritaglia foto",
                            tint = Color.White
                        )
                    }
                } else {
                    Text("+ SCATTA FOTO", fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            if (hasImage) {
                val currentPhotoUri = when {
                    processedImageFile != null -> Uri.fromFile(processedImageFile!!)
                    imageUri != null -> imageUri
                    existingImageUrl != null -> {
                        if (existingImageUrl!!.startsWith("content://") || existingImageUrl!!.startsWith("file://")) {
                            Uri.parse(existingImageUrl)
                        } else {
                            Uri.fromFile(java.io.File(existingImageUrl!!))
                        }
                    }
                    else -> null
                }
                
                if (currentPhotoUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentApiKey.isBlank()) {
                            Text(
                                text = "Configura API Key nelle impostazioni per identificare la specie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            when (val state = identificationState) {
                                is PlantViewModel.IdentificationState.Idle -> {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.identifyPlantSpecies(currentPhotoUri)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed)
                                    ) {
                                        Text("IDENTIFICA SPECIE CON AI", fontFamily = NothingFontFamily)
                                    }
                                }
                                is PlantViewModel.IdentificationState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                                    ) {
                                        CircularProgressIndicator(color = NothingRed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "IDENTIFICAZIONE IN CORSO...",
                                            fontFamily = NothingFontFamily,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                                is PlantViewModel.IdentificationState.Success -> {
                                    LaunchedEffect(state) {
                                        plantSpecies = state.species
                                        if (plantName.isBlank() && state.commonName.isNotBlank()) {
                                            plantName = state.commonName
                                        }
                                        aiDetectedIsMix = state.isMix
                                        aiDetectedPlantCount = state.plantCount
                                        userSaysIsMix = state.isMix
                                        userPlantCountInput = if (state.isMix) state.plantCount.toString() else "1"
                                        
                                        android.widget.Toast.makeText(
                                            context,
                                            "Identificata: ${state.species}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        viewModel.resetIdentificationState()
                                    }
                                }
                                is PlantViewModel.IdentificationState.Error -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = state.message.uppercase(),
                                            fontFamily = NothingFontFamily,
                                            color = NothingRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.identifyPlantSpecies(currentPhotoUri)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed)
                                        ) {
                                            Text("RIPROVA IDENTIFICAZIONE", fontFamily = NothingFontFamily)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (aiDetectedPlantCount > 1 || aiDetectedIsMix) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RILEVATO MIX DI PIANTE",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        color = NothingRed
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Siamo in presenza di un mix di piante?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = userSaysIsMix,
                            onCheckedChange = { userSaysIsMix = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NothingRed
                            )
                        )
                    }
                    
                    if (userSaysIsMix) {
                        OutlinedTextField(
                            value = userPlantCountInput,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    userPlantCountInput = newValue
                                }
                            },
                            label = { Text("Di quante piante?", fontFamily = NothingFontFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NothingRed,
                                focusedLabelColor = NothingRed,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                    
                    val expectedCount = aiDetectedPlantCount
                    val actualCount = if (userSaysIsMix) userPlantCountInput.toIntOrNull() ?: 0 else 1
                    
                    if (userSaysIsMix && actualCount != expectedCount) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingRed.copy(alpha = 0.1f))
                                .border(0.5.dp, NothingRed, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Il numero di piante inserito ($actualCount) non coincide con quello rilevato dall'AI ($expectedCount).",
                                style = MaterialTheme.typography.bodySmall,
                                color = NothingRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "SCHEDA DI CURA AI",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Genera le istruzioni botaniche personalizzate per la cura di questa pianta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stato di generazione
            when (val state = summaryState) {
                is PlantViewModel.SummaryState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        CircularProgressIndicator(color = NothingRed, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "GENERAZIONE SCHEDA IN CORSO...",
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                is PlantViewModel.SummaryState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingRed.copy(alpha = 0.1f))
                            .border(1.dp, NothingRed, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ERRORE AI",
                            fontFamily = NothingFontFamily,
                            color = NothingRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val expectedCount = aiDetectedPlantCount
                        val actualCount = if (userSaysIsMix) userPlantCountInput.toIntOrNull() ?: 0 else 1
                        val isMixCountValid = !userSaysIsMix || (actualCount == expectedCount)
                        
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val currentPhotoUri = processedImageFile?.let { Uri.fromFile(it) } ?: (if (isNewPhoto) imageUri else null) ?: existingImageUrl?.let { Uri.parse(it) }
                                viewModel.generateSummary(plantSpecies.ifBlank { plantName }, currentPhotoUri, isMix = userSaysIsMix)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed),
                            enabled = isMixCountValid,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("RIPROVA", fontFamily = NothingFontFamily)
                        }
                    }
                }
                else -> {
                    val expectedCount = aiDetectedPlantCount
                    val actualCount = if (userSaysIsMix) userPlantCountInput.toIntOrNull() ?: 0 else 1
                    val isMixCountValid = !userSaysIsMix || (actualCount == expectedCount)

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val currentPhotoUri = processedImageFile?.let { Uri.fromFile(it) } ?: (if (isNewPhoto) imageUri else null) ?: existingImageUrl?.let { Uri.parse(it) }
                            viewModel.generateSummary(plantSpecies.ifBlank { plantName }, currentPhotoUri, isMix = userSaysIsMix)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (plantSpecies.isNotBlank() || plantName.isNotBlank()) && isMixCountValid,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed)
                    ) {
                        Text(
                            text = if (aiSummaryState != null) "RIGENERA SCHEDA DI CURA" else "GENERA SCHEDA DI CURA CON AI",
                            fontFamily = NothingFontFamily
                        )
                    }
                }
            }

            // Visualizzazione scheda generata (se presente)
            if (aiSummaryState != null) {
                var light = ""
                var water = ""
                var soil = ""
                var fertilizer = ""
                var incompatibleWarning = ""
                var isJson = false
                
                try {
                    val json = org.json.JSONObject(aiSummaryState!!)
                    light = json.optString("light", "")
                    water = json.optString("water", "")
                    soil = json.optString("soil", "")
                    fertilizer = json.optString("fertilizer", "")
                    incompatibleWarning = json.optString("incompatibleWarning", "")
                    isJson = true
                } catch (e: Exception) {
                    isJson = false
                }
                
                if (isJson) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PREVIEW SCHEDA DI CURA",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        
                        if (incompatibleWarning.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingRed.copy(alpha = 0.1f))
                                    .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = incompatibleWarning.uppercase(),
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NothingRed
                                )
                            }
                        }
                        
                        if (light.isNotBlank()) TechnicalRowPreview(Icons.Default.WbSunny, "LUCE: $light")
                        if (water.isNotBlank()) TechnicalRowPreview(Icons.Default.WaterDrop, "ACQUA: $water")
                        if (soil.isNotBlank()) TechnicalRowPreview(Icons.Default.Grass, "TERRENO: $soil")
                        if (fertilizer.isNotBlank()) TechnicalRowPreview(Icons.Default.Science, "CONCIME: $fertilizer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            val expectedCount = aiDetectedPlantCount
            val actualCount = if (userSaysIsMix) userPlantCountInput.toIntOrNull() ?: 0 else 1
            val isMixCountValid = !userSaysIsMix || (actualCount == expectedCount)

            Button(
                onClick = { 
                    if (plantName.isNotBlank() || plantSpecies.isNotBlank()) {
                        val finalUri = if (processedImageFile != null) Uri.fromFile(processedImageFile!!) else (if (isNewPhoto) imageUri else null)
                        val isProcessed = processedImageFile != null
                        val originalUri = if (isNewPhoto) imageUri else null
                        
                        val currentId = plantId
                        val potDiameterInt = potDiameterStr.toIntOrNull()
                        if (currentId != null) {
                            viewModel.updatePlantData(currentId, plantName, plantSpecies, plantRoom, adoptionDate, finalUri, isAlreadyProcessed = isProcessed, newOriginalImageUri = originalUri, aiSummary = aiSummaryState, potDiameter = potDiameterInt) {
                                onBack()
                            }
                        } else {
                            viewModel.addPlant(plantName, plantSpecies, plantRoom, adoptionDate, finalUri, isAlreadyProcessed = isProcessed, originalImageUri = originalUri, aiSummary = aiSummaryState, potDiameter = potDiameterInt) {
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = (plantName.isNotBlank() || plantSpecies.isNotBlank()) && isMixCountValid,
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
            ) {
                Text(
                    text = "SALVA",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }

    if (showCropDialog && originalBitmap != null) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        val boxSizeDp = 280.dp
        val boxSizePx = remember(boxSizeDp) { with(density) { boxSizeDp.toPx() } }
        
        val W = originalBitmap.width.toFloat()
        val H = originalBitmap.height.toFloat()
        val baseScale = boxSizePx / maxOf(W, H)
        
        var isCropping by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Dialog(
            onDismissRequest = { 
                showCropDialog = false 
                imageUri = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "REGOLA FOTO",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Trascina per spostare, usa due dita per zoomare.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(boxSizeDp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .pointerInput(originalBitmap) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    val wScaled = W * baseScale * scale
                                    val hScaled = H * baseScale * scale
                                    val maxX = maxOf(0f, (wScaled - boxSizePx) / 2f)
                                    val maxY = maxOf(0f, (hScaled - boxSizePx) / 2f)
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                     )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                showCropDialog = false 
                                imageUri = null
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ANNULLA", fontFamily = NothingFontFamily)
                        }
                        
                        Button(
                            onClick = {
                                if (!isCropping) {
                                    isCropping = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val cropped = cropBitmap(originalBitmap, scale, offset, boxSizePx)
                                            if (cropped != null) {
                                                val tempFile = File(context.cacheDir, "temp_cropped_${System.currentTimeMillis()}.jpg")
                                                FileOutputStream(tempFile).use { out ->
                                                    cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                }
                                                withContext(Dispatchers.Main) {
                                                    croppedUri = Uri.fromFile(tempFile)
                                                    showCropDialog = false
                                                }
                                            } else {
                                                throw Exception("Ritaglio dell'immagine fallito (bitmap nullo).")
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                errorMessage = "Impossibile ritagliare o salvare la foto: ${e.localizedMessage}"
                                                showErrorDialog = true
                                                showCropDialog = false
                                                imageUri = null
                                            }
                                        } finally {
                                            withContext(Dispatchers.Main) {
                                                isCropping = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                            enabled = !isCropping
                        ) {
                            if (isCropping) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("CONFERMA", fontFamily = NothingFontFamily, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = false 
                errorMessage = null
            },
            title = {
                Text(
                    text = "ERRORE ELABORAZIONE FOTO",
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showErrorDialog = false 
                        errorMessage = null
                    }
                ) {
                    Text("OK", color = NothingRed, fontFamily = NothingFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        )
    }

    if (showDeleteDialog && plantId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "ELIMINA PIANTA",
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare questa pianta? Tutti i dati associati (cura, foto e promemoria) verranno persi per sempre.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlantById(plantId) {
                            showDeleteDialog = false
                            onBack()
                        }
                    }
                ) {
                    Text("ELIMINA", color = NothingRed, fontFamily = NothingFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ANNULLA", color = MaterialTheme.colorScheme.onBackground, fontFamily = NothingFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        )
    }
}

private fun cropBitmap(original: Bitmap, scale: Float, offset: Offset, boxSizePx: Float): Bitmap? {
    return try {
        val C = 1000
        val cropped = Bitmap.createBitmap(C, C, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        
        val W = original.width.toFloat()
        val H = original.height.toFloat()
        val baseScale = C.toFloat() / maxOf(W, H)
        
        val matrix = android.graphics.Matrix()
        matrix.postTranslate(-W / 2f, -H / 2f)
        
        val finalScale = baseScale * scale
        matrix.postScale(finalScale, finalScale)
        
        val scaleRatio = C.toFloat() / boxSizePx
        val finalTx = offset.x * scaleRatio
        val finalTy = offset.y * scaleRatio
        matrix.postTranslate(C / 2f + finalTx, C / 2f + finalTy)
        
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(original, matrix, paint)
        
        cropped
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun TechnicalRowPreview(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}
