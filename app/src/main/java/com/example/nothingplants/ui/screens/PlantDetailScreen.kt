package com.example.nothingplants.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
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
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

data class TimelineItem(
    val imagePath: String,
    val dateLabel: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantDetailScreen(
    plantId: Long,
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val plant by viewModel.getPlant(plantId).collectAsState(initial = null)
    val logs by viewModel.getWateringLogs(plantId).collectAsState(initial = emptyList())
    val lastLog = logs.firstOrNull()
    val activeWateringReminder by viewModel.getActiveWateringReminderForPlant(plantId).collectAsState(initial = null)
    val activeFertilizingReminder by viewModel.getActiveFertilizingReminderForPlant(plantId).collectAsState(initial = null)
    
    var summaryLoading by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showAiEvaluationSheet by remember { mutableStateOf(false) }
    var showAiLogEvaluationSheet by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Dettaglio", 
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (plant == null) return@Scaffold
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (plant?.imageUri != null) {
                                    showContextMenu = true
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (plant?.imageUri != null) {
                        AsyncImage(
                            model = plant?.imageUri,
                            contentDescription = "Plant image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Verifica salute ai", fontFamily = NothingFontFamily) },
                                onClick = {
                                    showContextMenu = false
                                    plant?.imageUri?.let { uri ->
                                         viewModel.analyzePhotoHealth(listOf(uri), plant?.species)
                                    }
                                }
                            )
                        }
                    } else {
                        Text("NESSUNA IMMAGINE", fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val displayName = if (plant?.name?.isNotBlank() == true) plant?.name?.uppercase() else plant?.species?.uppercase()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName ?: "",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (plant?.aiSummary != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                
                if (plant?.name?.isNotBlank() == true && !plant?.species.isNullOrBlank()) {
                    Text(
                        text = plant?.species?.uppercase() ?: "",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                val adoptionStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(plant?.adoptionDate ?: System.currentTimeMillis()))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ADOTTATA IL: $adoptionStr",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    if (!plant?.room.isNullOrBlank()) {
                        Text(
                            text = plant?.room?.uppercase() ?: "",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = NothingRed
                        )
                    }
                }
                
                if (plant?.potDiameter != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAMETRO VASO: ${plant?.potDiameter} CM",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Box Informativo Stato di Cura
                val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
                val nextWateringStr = remember(activeWateringReminder) {
                    activeWateringReminder?.let { dateFormat.format(java.util.Date(it.dueDate)) } ?: "NESSUNO IN PROGRAMMA"
                }
                val nextFertilizingStr = remember(activeFertilizingReminder) {
                    activeFertilizingReminder?.let { dateFormat.format(java.util.Date(it.dueDate)) } ?: "NESSUNO IN PROGRAMMA"
                }

                var fertilizerType = "NON SPECIFICATO (GENERA SCHEDA AI)"
                if (plant?.aiSummary != null) {
                    try {
                        val json = JSONObject(plant!!.aiSummary!!)
                        val rawFert = json.optString("fertilizer", "NON SPECIFICATO")
                        
                        // Estraiamo solo il tipo essenziale di concime eliminando virgole, punti o parole chiave temporali (es. ogni, da dare, in, etc.)
                        val cleanFert = rawFert.split(Regex("[,.\\n]|\\bogni\\b|\\bda somministrare\\b|\\bin\\b|\\buna volta\\b|\\bda dare\\b|\\bda applicare\\b", RegexOption.IGNORE_CASE))
                            .firstOrNull()?.trim() ?: rawFert
                        
                        fertilizerType = if (cleanFert.isNotBlank()) cleanFert.uppercase() else "NON SPECIFICATO"
                    } catch (e: Exception) {
                        fertilizerType = "NON SPECIFICATO"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "CONCIME DA USARE",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fertilizerType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PROSSIMA INNAFFIATURA",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextWateringStr,
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (activeWateringReminder != null) NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PROSSIMA CONCIMAZIONE",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.End)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextFertilizingStr,
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (activeFertilizingReminder != null) NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text(
                        text = "CURA",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (plant?.aiSummary != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var light = ""
                    var water = ""
                    var soil = ""
                    var fertilizer = ""
                    var incompatibleWarning = ""
                    var isJson = false
                    
                    try {
                        val json = JSONObject(plant!!.aiSummary!!)
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SCHEDA DI CURA AI",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            IconButton(onClick = { showAiEvaluationSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Dettagli AI",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (incompatibleWarning.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NothingRed.copy(alpha = 0.08f))
                                        .border(0.5.dp, NothingRed, RoundedCornerShape(12.dp))
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
                            if (light.isNotBlank()) TechnicalRow(Icons.Default.WbSunny, light)
                            if (water.isNotBlank()) TechnicalRow(Icons.Default.WaterDrop, water)
                            if (soil.isNotBlank()) TechnicalRow(Icons.Default.Grass, soil)
                            if (fertilizer.isNotBlank()) TechnicalRow(Icons.Default.Science, fertilizer)
                        }
                    }
                } else if (summaryLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NothingRed)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "GENERAZIONE IN CORSO...",
                                fontFamily = NothingFontFamily,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else if (summaryError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "ERRORE DI GENERAZIONE",
                                fontFamily = NothingFontFamily,
                                color = NothingRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Impossibile comunicare con Gemini. Controlla che la tua API Key nelle impostazioni sia corretta o riprova più tardi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Health Score (Ultimo)
                if (lastLog?.aiHealthScore != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI HEALTH SCORE: ",
                                fontFamily = NothingFontFamily,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${lastLog.aiHealthScore}/10",
                                fontFamily = NothingFontFamily,
                                color = NothingRed,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        IconButton(onClick = { showAiLogEvaluationSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Dettagli AI",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Text(
                        text = lastLog.aiNotes ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        if (showInfoSheet) {
            var infoText = plant?.aiSummary ?: ""
            try {
                val json = JSONObject(infoText)
                infoText = json.optString("info", "")
            } catch (e: Exception) {
                // If not JSON, use the whole text
            }
            
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "INFO PIANTA",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showAiEvaluationSheet) {
            val isLocationEnabled by viewModel.locationEnabled.collectAsState()
            val city by viewModel.locationCity.collectAsState()
            val lat by viewModel.locationLatitude.collectAsState()
            val lon by viewModel.locationLongitude.collectAsState()
            val hasPhoto = plant?.imageUri != null
            val species = plant?.species ?: ""
            
            ModalBottomSheet(
                onDismissRequest = { showAiEvaluationSheet = false },
                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF141414) else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "VALUTAZIONE AI",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fattori considerati dal modello AI per produrre questa scheda di cura:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AiFactorItem("Specie Botanica", species.ifBlank { plant?.name ?: "-" })
                    AiFactorItem(
                        "Geolocalizzazione",
                        if (isLocationEnabled && city.isNotBlank()) "ATTIVA ($city)" else "NON ATTIVA (Clima generico)"
                    )
                    if (isLocationEnabled && lat.isNotBlank() && lon.isNotBlank()) {
                        AiFactorItem("Coordinate GPS", "Lat: $lat, Lon: $lon")
                    }
                    AiFactorItem("Analisi Visiva (Foto)", if (hasPhoto) "CONSIDERATA (Foto pianta caricata)" else "NON DISPONIBILE (Solo dati testuali)")
                    AiFactorItem("Modello AI Utilizzato", "Gemini 3.5 Flash (Botanist Expert System)")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showAiLogEvaluationSheet && lastLog != null) {
            val isLocationEnabled by viewModel.locationEnabled.collectAsState()
            val city by viewModel.locationCity.collectAsState()
            val hasPhoto = lastLog.imagePath != null
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastLog.timestamp))
            
            ModalBottomSheet(
                onDismissRequest = { showAiLogEvaluationSheet = false },
                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF141414) else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "VALUTAZIONE AI SALUTE",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fattori considerati dal modello AI per valutare lo stato di salute e suggerire il prossimo intervallo:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AiFactorItem("Data di Rilevamento", formattedDate)
                    AiFactorItem("Tipo di Cura Registrato", if (lastLog.logType == "WATERING") "ANNAFFIATURA" else if (lastLog.logType == "FERTILIZING") "CONCIMAZIONE" else lastLog.logType)
                    AiFactorItem("Foto dello Stato Corrente", if (hasPhoto) "FOTO ANALIZZATA (Rilevamento macchie/secchezza)" else "NON FORNITA (Valutazione statistica)")
                    AiFactorItem(
                        "Geolocalizzazione",
                        if (isLocationEnabled && city.isNotBlank()) "ATTIVA ($city)" else "NON ATTIVA (Clima generico)"
                    )
                    if (lastLog.logType == "WATERING") {
                        AiFactorItem("Storico Annaffiature", "Calcolato trend dinamico sugli eventi precedenti")
                    } else if (lastLog.logType == "FERTILIZING") {
                        AiFactorItem("Regola di Concimazione", "Verificata stagionalità su scheda di cura AI")
                    }
                    AiFactorItem("Modello AI Utilizzato", "Gemini 3.5 Flash (Botanist Expert System)")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
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

@Composable
fun AiFactorItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = NothingFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}

@Composable
fun TechnicalRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        )
    }
}
