package com.example.nothingplants.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import com.example.nothingplants.data.WateringLog
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    plantId: Long,
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onNavigateToGrowthDiary: (Long) -> Unit
) {
    val plant by viewModel.getPlant(plantId).collectAsState(initial = null)
    val logs by viewModel.getWateringLogs(plantId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)
    val cardBorder = if (isDark) Color(0xFF181818) else Color(0xFFE0E0E0)

    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }
    var contextMenuImageUri by remember { mutableStateOf<String?>(null) }

    val timelineItems = remember(plant, logs) {
        val items = mutableListOf<TimelineItem>()
        val adoptionImg = plant?.imageUri
        val adoptionDate = plant?.adoptionDate
        if (adoptionImg != null && adoptionDate != null) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(adoptionDate))
            items.add(
                TimelineItem(
                    imagePath = adoptionImg,
                    dateLabel = dateStr,
                    timestamp = adoptionDate
                )
            )
        }
        logs.sortedBy { it.timestamp }.forEach { log ->
            val path = log.imagePath
            if (!path.isNullOrBlank()) {
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(log.timestamp))
                items.add(
                    TimelineItem(
                        imagePath = path,
                        dateLabel = dateStr,
                        timestamp = log.timestamp
                    )
                )
            }
        }
        items
    }



    val wateringLogs = remember(logs) { logs.filter { it.logType == "WATERING" }.sortedByDescending { it.timestamp } }
    val fertilizingLogs = remember(logs) { logs.filter { it.logType == "FERTILIZING" }.sortedByDescending { it.timestamp } }
    val repottingLogs = remember(logs) { logs.filter { it.logType == "REPOTTING" }.sortedByDescending { it.timestamp } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Cura", 
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // SEZIONE 1: Diario di crescita
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToGrowthDiary(plantId) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIARIO DI CRESCITA",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "VEDI TUTTO >",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingRed
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(110.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                        .border(
                                            1.dp, 
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), 
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            onNavigateToGrowthDiary(plantId)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ FOTO",
                                        fontFamily = NothingFontFamily,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NothingRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AGGIUNGI",
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                        
                        items(timelineItems) { item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(110.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .border(
                                            1.dp, 
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), 
                                            RoundedCornerShape(12.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                selectedImageForPreview = item.imagePath
                                            },
                                            onLongClick = {
                                                contextMenuImageUri = item.imagePath
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = item.imagePath,
                                        contentDescription = "Foto timeline ${item.dateLabel}",
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
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.dateLabel,
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "STORICO CURE",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            DotMatrixGraph(
                title = "STORICO CURE",
                wateringLogs = wateringLogs,
                fertilizingLogs = fertilizingLogs,
                repottingLogs = repottingLogs,
                viewModel = viewModel,
                context = context,
                accentColor = NothingRed
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var isWateringAnalyzing by remember { mutableStateOf(false) }
                Button(
                    onClick = { 
                        val currentPlant = plant
                        if (currentPlant != null) {
                            isWateringAnalyzing = true
                            viewModel.addCareLogWithoutPhoto(currentPlant, "WATERING") {
                                isWateringAnalyzing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    enabled = !isWateringAnalyzing
                ) {
                    Text(
                        text = if (isWateringAnalyzing) "ANALISI..." else "ANNAFFIATO",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                
                var isFertilizingAnalyzing by remember { mutableStateOf(false) }
                Button(
                    onClick = { 
                        val currentPlant = plant
                        if (currentPlant != null) {
                            isFertilizingAnalyzing = true
                            viewModel.addCareLogWithoutPhoto(currentPlant, "FERTILIZING") {
                                isFertilizingAnalyzing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFE5E5E5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
                    ),
                    enabled = !isFertilizingAnalyzing
                ) {
                    Text(
                        text = if (isFertilizingAnalyzing) "ANALISI..." else "CONCIMATO",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

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
fun DotMatrixGraph(
    title: String,
    wateringLogs: List<WateringLog>,
    fertilizingLogs: List<WateringLog>,
    repottingLogs: List<WateringLog>,
    viewModel: PlantViewModel,
    context: android.content.Context,
    accentColor: Color = NothingRed,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF5F5F5)
    val cardBorder = if (isDark) Color(0xFF181818) else Color(0xFFE0E0E0)
    
    val dotBgUnlit = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE5E5E5)
    val dotBorderUnlit = if (isDark) Color(0xFF282828) else Color(0xFFD0D0D0)

    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { 
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }

    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.ITALIAN) }
    val monthYearText = remember(currentMonth) { 
        monthYearFormatter.format(currentMonth.time).uppercase() 
    }

    val maxDaysInMonth = remember(currentMonth) {
        currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val offset = remember(currentMonth) {
        val startCal = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dayOfWeek1st = startCal.get(Calendar.DAY_OF_WEEK)
        when (dayOfWeek1st) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    val rowsNeeded = remember(offset, maxDaysInMonth) {
        val totalCells = offset + maxDaysInMonth
        (totalCells + 6) / 7
    }

    val activeWateringDays = remember(wateringLogs, currentMonth) {
        val daysSet = mutableSetOf<Int>()
        val startCalendar = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        val startTime = startCalendar.timeInMillis
        val endTime = endCalendar.timeInMillis
        
        wateringLogs.filter { it.timestamp in startTime..endTime }.forEach { log ->
            val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val day = logCal.get(Calendar.DAY_OF_MONTH)
            daysSet.add(day)
        }
        daysSet
    }

    val activeFertilizingDays = remember(fertilizingLogs, currentMonth) {
        val daysSet = mutableSetOf<Int>()
        val startCalendar = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        val startTime = startCalendar.timeInMillis
        val endTime = endCalendar.timeInMillis
        
        fertilizingLogs.filter { it.timestamp in startTime..endTime }.forEach { log ->
            val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val day = logCal.get(Calendar.DAY_OF_MONTH)
            daysSet.add(day)
        }
        daysSet
    }

    val combinedLogs = remember(wateringLogs, fertilizingLogs, repottingLogs) {
        (wateringLogs + fertilizingLogs + repottingLogs).sortedByDescending { it.timestamp }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAiLogEvaluationSheet by remember { mutableStateOf(false) }
    var selectedLogForInfoSheet by remember { mutableStateOf<WateringLog?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Box vuoto a sinistra per centrare la selezione del mese compensando l'icona a destra
                Box(modifier = Modifier.width(48.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "<",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    
                    Text(
                        text = monthYearText,
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(
                        onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = ">",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                
                IconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Storico date",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(17.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Intestazione giorni della settimana (L, M, M, G, V, S, D)
                Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                    val daysOfWeekLabels = listOf("L", "M", "M", "G", "V", "S", "D")
                    daysOfWeekLabels.forEach { label ->
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                for (row in 0 until rowsNeeded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayIndex = cellIndex - offset + 1
                            if (dayIndex in 1..maxDaysInMonth) {
                                val isWatered = activeWateringDays.contains(dayIndex)
                                val isFertilized = activeFertilizingDays.contains(dayIndex)
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isWatered) accentColor else dotBgUnlit)
                                        .then(
                                            if (isFertilized) {
                                                Modifier.border(
                                                    2.dp, 
                                                    if (isDark) Color.White else Color.Black,
                                                    CircleShape
                                                )
                                            } else if (!isWatered) {
                                                Modifier.border(
                                                    1.5.dp, 
                                                    dotBorderUnlit,
                                                    CircleShape
                                                )
                                            } else Modifier
                                        )
                                )
                            } else {
                                Box(modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF141414) else Color(0xFFFAFAFA),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (combinedLogs.isEmpty()) {
                    Text(
                        text = "NESSUN EVENTO REGISTRATO",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(combinedLogs) { log ->
                            LogItemRow(log, viewModel, context) { selectedLog ->
                                selectedLogForInfoSheet = selectedLog
                                showAiLogEvaluationSheet = true
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAiLogEvaluationSheet && selectedLogForInfoSheet != null) {
        val log = selectedLogForInfoSheet!!
        val isLocationEnabled by viewModel.locationEnabled.collectAsState()
        val city by viewModel.locationCity.collectAsState()
        val hasPhoto = log.imagePath != null
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
        
        ModalBottomSheet(
            onDismissRequest = { showAiLogEvaluationSheet = false },
            containerColor = if (isDark) Color(0xFF141414) else Color(0xFFFAFAFA),
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
                
                AiFactorItemRow("Data di Rilevamento", formattedDate)
                AiFactorItemRow("Tipo di Cura Registrato", if (log.logType == "WATERING") "ANNAFFIATURA" else if (log.logType == "FERTILIZING") "CONCIMAZIONE" else log.logType)
                AiFactorItemRow("Foto dello Stato Corrente", if (hasPhoto) "FOTO ANALIZZATA" else "NON FORNITA (Valutazione statistica)")
                AiFactorItemRow(
                    "Geolocalizzazione",
                    if (isLocationEnabled && city.isNotBlank()) "ATTIVA ($city)" else "NON ATTIVA"
                )
                if (log.logType == "WATERING") {
                    AiFactorItemRow("Storico Annaffiature", "Calcolato trend dinamico sugli eventi precedenti")
                } else if (log.logType == "FERTILIZING") {
                    AiFactorItemRow("Regola di Concimazione", "Verificata stagionalità su scheda di cura AI")
                }
                AiFactorItemRow("Modello AI Utilizzato", "Gemini 3.5 Flash")
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItemRow(
    log: WateringLog,
    viewModel: PlantViewModel,
    context: android.content.Context,
    onShowAiLogEvaluation: (WateringLog) -> Unit
) {
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    var showMenu by remember { mutableStateOf(false) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showMenu = true }
                )
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Indicatore visivo del tipo di log
                val indicatorColor = when (log.logType) {
                    "WATERING" -> NothingRed
                    "FERTILIZING" -> (if (isDark) Color.White else Color.Black)
                    "REPOTTING" -> NothingRed
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                        .then(
                            if (log.logType == "FERTILIZING") {
                                Modifier.border(1.dp, if (isDark) Color.White else Color.Black, CircleShape)
                            } else if (log.logType == "REPOTTING") {
                                Modifier.border(1.5.dp, if (isDark) Color.White else Color.Black, CircleShape)
                            } else Modifier
                        )
                )
                
                val typeLabel = when (log.logType) {
                    "WATERING" -> "ANNAFFIATURA"
                    "FERTILIZING" -> "CONCIMAZIONE"
                    "REPOTTING" -> "RINVASO"
                    else -> log.logType.uppercase()
                }
                
                Text(
                    text = typeLabel,
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = indicatorColor
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (log.logType == "REPOTTING" && log.newPotDiameter != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${log.newPotDiameter} CM)",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingRed
                    )
                }
            }
            
            if (log.logType != "REPOTTING" && log.aiHealthScore != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Score: ${log.aiHealthScore}",
                        fontFamily = NothingFontFamily,
                        color = NothingRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onShowAiLogEvaluation(log) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Dettagli AI",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("MODIFICA", fontFamily = NothingFontFamily) },
                onClick = {
                    showMenu = false
                    val calendar = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    
                    val timePickerDialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            viewModel.updateWateringLogTimestamp(log, calendar.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )
                    
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            timePickerDialog.show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    datePickerDialog.show()
                }
            )
            DropdownMenuItem(
                text = { Text("ELIMINA", fontFamily = NothingFontFamily, color = NothingRed) },
                onClick = {
                    showMenu = false
                    viewModel.deleteWateringLog(log)
                }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
}

@Composable
fun AiFactorItemRow(label: String, value: String) {
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
