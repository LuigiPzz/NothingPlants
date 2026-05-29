package com.example.nothingplants.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.nothingplants.ui.CameraUtils
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantHealthCheckScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val imageUris = remember { mutableStateListOf<Uri>() }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var speciesName by remember { mutableStateOf("") }
    var showSourceDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val photoHealthState by viewModel.photoHealthState.collectAsStateWithLifecycle()
    val currentApiKey by viewModel.currentApiKey.collectAsStateWithLifecycle()
    val currentApiKey2 by viewModel.currentApiKey2.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    var showPlantSelectDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            imageUris.add(tempUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris.addAll(uris)
        }
    }

    // Reset dello stato dell'analisi quando si entra o si esce dalla schermata
    LaunchedEffect(Unit) {
        viewModel.resetPhotoHealthState()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetPhotoHealthState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Verifica Salute AI", 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Torna indietro", 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (photoHealthState is PlantViewModel.PhotoHealthState.Idle) {
                Text(
                    text = "DIAGNOSTICA STAND-ALONE",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = NothingRed
                )
                
                Text(
                    text = "Scatta o seleziona una o più foto di una pianta per analizzare le sue foglie, i rami e il terreno alla ricerca di problemi, criticità e consigli botanici personalizzati.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Box per la foto
                if (imageUris.isNotEmpty()) {
                    Text(
                        text = "FOTO SELEZIONATE (${imageUris.size})",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(imageUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Anteprima pianta da analizzare",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Pulsante Elimina in alto a destra
                                IconButton(
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        imageUris.remove(uri) 
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Rimuovi foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        // Bottone per aggiungere un'altra foto
                        item {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSourceDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+", fontSize = 32.sp, fontFamily = NothingFontFamily, color = NothingRed)
                                    Text("AGGIUNGI", fontSize = 11.sp, fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "NESSUNA FOTO SELEZIONATA",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val uri = CameraUtils.createTempImageUri(context)
                                        tempUri = uri
                                        cameraLauncher.launch(uri)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SCATTA", fontFamily = NothingFontFamily)
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        galleryLauncher.launch("image/*")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("GALLERIA", fontFamily = NothingFontFamily)
                                }
                            }
                        }
                    }
                }

                if (imageUris.isNotEmpty()) {
                    OutlinedTextField(
                        value = speciesName,
                        onValueChange = { speciesName = it },
                        label = { Text("Specie della pianta (opzionale)", fontFamily = NothingFontFamily) },
                        placeholder = { Text("es. Sansevieria, Monstera...", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            focusedLabelColor = NothingRed,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val apiConfigured = currentApiKey.isNotBlank() || currentApiKey2.isNotBlank()

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (apiConfigured) {
                                viewModel.analyzePhotoHealth(imageUris.map { it.toString() }, speciesName.ifBlank { null })
                            } else {
                                Toast.makeText(context, "Configura l'API Key nelle impostazioni per procedere.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        enabled = imageUris.isNotEmpty()
                    ) {
                        Text(
                            text = "VERIFICA SALUTE AI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    if (!apiConfigured) {
                        Text(
                            text = "Nota: Per effettuare l'analisi è necessario configurare la chiave API di Gemini nelle impostazioni dell'app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NothingRed,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            } else {
                // Visualizzazione degli stati di caricamento, successo ed errore
                when (val state = photoHealthState) {
                    is PlantViewModel.PhotoHealthState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NothingRed)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "ANALISI VISIVA IN CORSO...",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "L'AI sta analizzando i colori, lo stato delle foglie ed eventuali anomalie visibili. Attendi un momento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    is PlantViewModel.PhotoHealthState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "ERRORE DI ANALISI",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                color = NothingRed
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    .border(0.5.dp, NothingRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.resetPhotoHealthState() 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ANNULLA", fontFamily = NothingFontFamily)
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.analyzePhotoHealth(imageUris.map { it.toString() }, speciesName.ifBlank { null })
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                                ) {
                                    Text("RIPROVA", fontFamily = NothingFontFamily, color = Color.White)
                                }
                            }
                        }
                    }
                    is PlantViewModel.PhotoHealthState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DIAGNOSI SALUTE AI",
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NothingRed
                                )
                                
                                Text(
                                    text = "ANALISI COMPLETATA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }

                            // Galleria delle foto analizzate e possibilità di aggiungerne altre prima dell'associazione
                            Text(
                                text = "FOTO DI QUESTA SESSIONE (${imageUris.size})",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                items(imageUris) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Foto analizzata",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                imageUris.remove(uri)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Rimuovi foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Permettiamo all'utente di aggiungere altre foto anche DOPO l'analisi prima di salvare
                                item {
                                    Box(
                                        modifier = Modifier
                                            .width(90.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showSourceDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("+", fontSize = 24.sp, fontFamily = NothingFontFamily, color = NothingRed)
                                            Text("AGGIUNGI", fontSize = 9.sp, fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }

                            // Report Diagnostico in Markdown
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                    .border(
                                        width = 0.5.dp, 
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), 
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                MarkdownText(
                                    text = state.report,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Bottone per ricominciare
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    imageUris.clear()
                                    speciesName = ""
                                    viewModel.resetPhotoHealthState()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
                            ) {
                                Text(
                                    text = "NUOVA ANALISI",
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.background
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottone per associare a una pianta
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showPlantSelectDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                                enabled = imageUris.isNotEmpty()
                            ) {
                                Text(
                                    text = "ASSOCIA A UNA PIANTA",
                                    fontFamily = NothingFontFamily,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = {
                Text(
                    text = "Aggiungi Foto",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Vuoi scattare una foto con la fotocamera o sceglierne una o più dalla galleria?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("GALLERIA", fontFamily = NothingFontFamily, color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val uri = CameraUtils.createTempImageUri(context)
                    tempUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Text("FOTOCAMERA", fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showPlantSelectDialog) {
        AlertDialog(
            onDismissRequest = { showPlantSelectDialog = false },
            title = {
                Text(
                    text = "Associa a una Pianta",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (plants.isEmpty()) {
                    Text(
                        text = "Nessuna pianta registrata.",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(plants) { plant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPlantSelectDialog = false
                                            if (imageUris.isNotEmpty()) {
                                                viewModel.associateHealthCheckToPlant(
                                                    plantId = plant.id,
                                                    imageUris = imageUris.toList(),
                                                    report = (photoHealthState as PlantViewModel.PhotoHealthState.Success).report
                                                ) {
                                                    Toast.makeText(context, "Diagnosi e foto associate al diario di ${plant.name}", Toast.LENGTH_SHORT).show()
                                                    imageUris.clear()
                                                    speciesName = ""
                                                    viewModel.resetPhotoHealthState()
                                                }
                                            }
                                        }
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = plant.name,
                                            fontFamily = NothingFontFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (!plant.species.isNullOrBlank()) {
                                            Text(
                                                text = plant.species,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlantSelectDialog = false }) {
                    Text("ANNULLA", fontFamily = NothingFontFamily, color = NothingRed)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
