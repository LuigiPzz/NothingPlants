package com.example.nothingplants.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.FertilizerSummaryItem
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FertilizerSummaryScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    val cachedSummary by viewModel.fertilizerSummaryCache.collectAsStateWithLifecycle(initialValue = null)
    var isLoading by remember { mutableStateOf(false) }
    var showAiFertilizerEvaluationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(cachedSummary) {
        if (cachedSummary == null) {
            isLoading = true
            viewModel.refreshFertilizerSummary {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tipi di Concime", 
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NothingRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ANALISI DEI CONCIMI IN CORSO...",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                val currentItems = cachedSummary ?: emptyList()
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "NESSUNA PIANTA TROVATA",
                                fontFamily = NothingFontFamily,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aggiungi delle piante ed effettua l'analisi AI per visualizzare il riepilogo dei concimi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Di seguito trovi il riassunto di tutti i concimi richiesti per le tue piante, calcolato in base alle esigenze indicate dai report di cura.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { showAiFertilizerEvaluationSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Dettagli AI",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            items(currentItems) { item ->
                                FertilizerCard(item)
                            }
                        }
                    }
                }
            }
        }

        if (showAiFertilizerEvaluationSheet) {
            val isLocationEnabled by viewModel.locationEnabled.collectAsState()
            val city by viewModel.locationCity.collectAsState()
            
            ModalBottomSheet(
                onDismissRequest = { showAiFertilizerEvaluationSheet = false },
                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF141414) else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "VALUTAZIONE AI CONCIMI",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fattori considerati dal modello AI per raggruppare ed analizzare i concimi delle tue piante:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AiFertilizerFactorItem("Input di Elaborazione", "Testi descrittivi concime estratti da tutte le schede di cura")
                    AiFertilizerFactorItem(
                        "Geolocalizzazione",
                        if (isLocationEnabled && city.isNotBlank()) "CONSIDERATA (Tramite schede di cura attive a $city)" else "NON ATTIVA"
                    )
                    AiFertilizerFactorItem("Obiettivo dell'AI", "Classificare concimi generici in categorie commerciali omogenee")
                    AiFertilizerFactorItem("Algoritmo di Raggruppamento", "Gemini 3.5 Flash Classifier")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FertilizerCard(item: FertilizerSummaryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp, 
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), 
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.fertilizerType,
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val labelPianta = if (item.plantsCount == 1) "1 PIANTA" else "${item.plantsCount} PIANTE"
            Text(
                text = "NECESSARIO PER $labelPianta",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = NothingRed
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            item.plantNames.forEach { plantName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = plantName.uppercase(),
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AiFertilizerFactorItem(label: String, value: String) {
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
