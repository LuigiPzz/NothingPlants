package com.example.nothingplants.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.nothingplants.ui.CameraUtils
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GrowthDiaryScreen(
    plantId: Long,
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val plant by viewModel.getPlant(plantId).collectAsState(initial = null)
    val logs by viewModel.getWateringLogs(plantId).collectAsState(initial = emptyList())
    
    val currentLux by viewModel.currentLux.collectAsState()
    val currentAzimuth by viewModel.currentAzimuth.collectAsState()
    
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }
    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }
    var contextMenuImageUri by remember { mutableStateOf<String?>(null) }
    var logToDelete by remember { mutableStateOf<com.example.nothingplants.data.WateringLog?>(null) }
    var selectedLogForInfo by remember { mutableStateOf<com.example.nothingplants.data.WateringLog?>(null) }
    
    var showTrendReport by remember { mutableStateOf(false) }
    var trendReportText by remember { mutableStateOf<String?>(null) }
    var isGeneratingTrend by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    // Registra e rimuovi i sensori a seconda del ciclo di vita dello schermo
    DisposableEffect(Unit) {
        viewModel.startSensors()
        onDispose {
            viewModel.stopSensors()
        }
    }

    val timelineItems = remember(plant, logs) {
        val items = mutableListOf<TimelineItemData>()
        // Foto Adozione
        val adoptionImg = plant?.imageUri
        val adoptionDate = plant?.adoptionDate
        if (adoptionImg != null && adoptionDate != null) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(adoptionDate))
            items.add(
                TimelineItemData(
                    imagePath = adoptionImg,
                    dateLabel = dateStr,
                    isAdoption = true,
                    logId = null
                )
            )
        }
        // Log con foto
        logs.sortedByDescending { it.timestamp }.forEach { log ->
            val path = log.imagePath
            if (!path.isNullOrBlank()) {
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                items.add(
                    TimelineItemData(
                        imagePath = path,
                        dateLabel = dateStr,
                        isAdoption = false,
                        logId = log.id,
                        rawLog = log
                    )
                )
            }
        }
        items
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            isSavingPhoto = true
            viewModel.addGrowthPhoto(plantId, tempUri!!, currentLux, currentAzimuth) {
                isSavingPhoto = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Diario di Crescita", 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Indietro", 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!isSavingPhoto) {
                FloatingActionButton(
                    onClick = {
                        val uri = CameraUtils.createTempImageUri(context)
                        tempUri = uri
                        cameraLauncher.launch(uri)
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Scatta Foto")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSavingPhoto) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NothingRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ANALISI IMMAGINE E SALVATAGGIO AI...",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                ) {


                    // Pulsante Analisi Andamento AI
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Button(
                            onClick = {
                                isGeneratingTrend = true
                                trendReportText = null
                                viewModel.generateGrowthTrendAnalysis(plantId) { report ->
                                    isGeneratingTrend = false
                                    trendReportText = report
                                    showTrendReport = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Trending Up",
                                    tint = NothingRed
                                )
                                Text(
                                    text = "ANALISI ANDAMENTO AI",
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }

                    // Intestazione scatti
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Text(
                            text = "SCATTI DI CRESCITA E CURA",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (timelineItems.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "NESSUNA FOTO NEL DIARIO",
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Clicca sul pulsante in basso per scattare la prima foto di crescita della tua pianta.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(timelineItems) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    .border(
                                        0.5.dp, 
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), 
                                        RoundedCornerShape(16.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            selectedImageForPreview = item.imagePath
                                        },
                                        onLongClick = {
                                            contextMenuImageUri = item.imagePath
                                        }
                                    )
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = item.imagePath,
                                        contentDescription = "Foto crescita",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    DropdownMenu(
                                        expanded = contextMenuImageUri == item.imagePath,
                                        onDismissRequest = { contextMenuImageUri = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Verifica salute ai", fontFamily = NothingFontFamily) },
                                            onClick = {
                                                contextMenuImageUri = null
                                                viewModel.analyzePhotoHealth(listOf(item.imagePath), plant?.species)
                                            }
                                        )
                                    }
                                    
                                    // Consenti l'eliminazione solo dei log aggiuntivi e non della foto profilo
                                    if (!item.isAdoption && item.rawLog != null) {
                                        IconButton(
                                            onClick = { logToDelete = item.rawLog },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(32.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Elimina foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.dateLabel,
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                val typeLabel = if (item.isAdoption) {
                                    "ADOZIONE"
                                } else {
                                    when (item.rawLog?.logType) {
                                        "GROWTH" -> "CRESCITA"
                                        "WATERING" -> "ANNAFFIATURA"
                                        "FERTILIZING" -> "CONCIMAZIONE"
                                        else -> "RILEVAMENTO"
                                    }
                                }
                                Text(
                                    text = typeLabel,
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isAdoption) NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                                
                                // Dati fisici dei sensori
                                val rawLog = item.rawLog
                                if (rawLog != null && (rawLog.luxValue != null || !rawLog.compassDirection.isNullOrBlank())) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val luxStr = if (rawLog.luxValue != null) "☀️ ${Math.round(rawLog.luxValue)} lx" else ""
                                    val dirStr = if (!rawLog.compassDirection.isNullOrBlank()) "🧭 ${rawLog.compassDirection}" else ""
                                    val physicalData = listOf(luxStr, dirStr).filter { it.isNotEmpty() }.joinToString(" | ")
                                    
                                    Text(
                                        text = physicalData,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                                
                                // Score Salute AI + Info button
                                if (rawLog != null && (rawLog.aiHealthScore != null || !rawLog.aiNotes.isNullOrBlank())) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NothingRed.copy(alpha = 0.08f))
                                            .border(0.5.dp, NothingRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        val scoreText = if (rawLog.aiHealthScore != null) "SALUTE: ${rawLog.aiHealthScore}/10" else "DIAGNOSI AI"
                                        Text(
                                            text = scoreText,
                                            fontFamily = NothingFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NothingRed
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Dettagli AI",
                                            tint = NothingRed,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    selectedLogForInfo = rawLog
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Dialog Anteprima Ingrandita
        if (selectedImageForPreview != null) {
            Dialog(
                onDismissRequest = { selectedImageForPreview = null }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedImageForPreview,
                            contentDescription = "Foto ingrandita",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        IconButton(
                            onClick = { selectedImageForPreview = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Dialog Conferma Cancellazione
        if (logToDelete != null) {
            AlertDialog(
                onDismissRequest = { logToDelete = null },
                title = {
                    Text(
                        "ELIMINA FOTO",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    Text(
                        "Sei sicuro di voler eliminare questa foto dal Diario di Crescita? L'azione non è reversibile.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteWateringLog(logToDelete!!)
                            logToDelete = null
                        }
                    ) {
                        Text("ELIMINA", color = NothingRed, fontFamily = NothingFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { logToDelete = null }) {
                        Text("ANNULLA", color = MaterialTheme.colorScheme.onBackground, fontFamily = NothingFontFamily)
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            )
        }

        // Overlay Dialog di Caricamento per Trend Report
        if (isGeneratingTrend) {
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NothingRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ELABORAZIONE TREND AI...",
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "L'AI sta analizzando la sequenza cronologica di foto per tracciare lo sviluppo biologico...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Sheet Informativo AI per singola foto
        if (selectedLogForInfo != null) {
            InfoBottomSheet(
                log = selectedLogForInfo!!,
                viewModel = viewModel,
                onDismiss = { selectedLogForInfo = null }
            )
        }

        // Bottom Sheet Report Andamento Temporale
        if (showTrendReport && trendReportText != null) {
            TrendReportBottomSheet(
                reportText = trendReportText!!,
                onDismiss = {
                    showTrendReport = false
                    trendReportText = null
                }
            )
        }

        val photoHealthState by viewModel.photoHealthState.collectAsState()
        if (photoHealthState !is PlantViewModel.PhotoHealthState.Idle) {
            PhotoHealthReportBottomSheet(
                state = photoHealthState,
                onDismiss = { viewModel.resetPhotoHealthState() }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    log: com.example.nothingplants.data.WateringLog,
    viewModel: PlantViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Text(
                text = "VALUTAZIONE BOTANICA AI",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.titleLarge,
                color = NothingRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                    .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STATO SALUTE AI",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.aiNotes ?: "Nessuna diagnosi disponibile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (log.aiHealthScore != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NothingRed.copy(alpha = 0.1f))
                            .border(1.dp, NothingRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${log.aiHealthScore}",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            color = NothingRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "FATTORI CONSIDERATI DALL'AI:",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Lista dei fattori
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Immagine
                FattoreRow(
                    titolo = "Analisi Visiva Foto",
                    descrizione = "Valutazione morfologica di foglie, rami e fusto per determinare appassimenti o parassiti."
                )
                
                // Lux
                val luxText = if (log.luxValue != null) {
                    "${String.format(Locale.getDefault(), "%.1f", log.luxValue)} Lux. Consente all'AI di stimare se la luce nella stanza è sufficiente rispetto ai requisiti biologici."
                } else {
                    "Non rilevato. Il sensore di luce ambientale dello smartphone non ha registrato alcun valore o non era disponibile."
                }
                FattoreRow(
                    titolo = "Luminosità Ambientale",
                    descrizione = luxText
                )
                
                // Orientamento
                val compassText = if (!log.compassDirection.isNullOrBlank()) {
                    "Esposizione a ${log.compassDirection}. Indica l'orientamento della finestra, fondamentale per calcolare il ciclo di insolazione."
                } else {
                    "Non rilevato. La bussola del telefono non è stata caricata o non era abilitata."
                }
                FattoreRow(
                    titolo = "Esposizione Cardinale",
                    descrizione = compassText
                )
                
                // Geolocation
                val isLocationEnabled by viewModel.locationEnabled.collectAsState()
                val city by viewModel.locationCity.collectAsState()
                val geoText = if (isLocationEnabled && city.isNotBlank()) {
                    "Posizione impostata su $city. Fornisce all'AI indicazioni su stagionalità reale, umidità esterna e temperature locali."
                } else {
                    "Non abilitata nelle impostazioni. Gemini ha effettuato una valutazione climatica generica di fallback."
                }
                FattoreRow(
                    titolo = "Localizzazione Geografica",
                    descrizione = geoText
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "CHIUDI",
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

@Composable
fun FattoreRow(titolo: String, descrizione: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.titleMedium,
            color = NothingRed
        )
        Column {
            Text(
                text = titolo.uppercase(),
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = descrizione,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendReportBottomSheet(
    reportText: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Trend",
                    tint = NothingRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ANALISI ANDAMENTO TEMPORALE",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = NothingRed
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = reportText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "CHIUDI",
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

private data class TimelineItemData(
    val imagePath: String,
    val dateLabel: String,
    val isAdoption: Boolean,
    val logId: Long?,
    val rawLog: com.example.nothingplants.data.WateringLog? = null
)
