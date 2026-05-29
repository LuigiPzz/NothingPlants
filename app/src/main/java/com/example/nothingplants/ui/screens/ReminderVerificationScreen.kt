package com.example.nothingplants.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.ReminderProposal
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderVerificationScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    var proposals by remember { mutableStateOf<List<ReminderProposal>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showAiProposalEvaluationSheet by remember { mutableStateOf(false) }
    var selectedProposalForInfoSheet by remember { mutableStateOf<ReminderProposal?>(null) }

    LaunchedEffect(Unit) {
        viewModel.verifyAllReminders { result ->
            proposals = result
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Verifica Promemoria", 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
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
                        "ANALISI DEI PROMEMORIA IN CORSO...",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else if (isSaving) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NothingRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "APPLICAZIONE DELLE MODIFICHE...",
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                val currentProposals = proposals ?: emptyList()
                if (currentProposals.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "TUTTI I PROMEMORIA COERENTI",
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Le scadenze dei promemoria correnti sono allineate con le indicazioni dei report di cura.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "L'analisi ha rilevato delle discrepanze tra i promemoria attuali e le frequenze ideali descritte nelle schede di cura delle tue piante.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(currentProposals) { proposal ->
                                ProposalItemCard(proposal) { selectedProposal ->
                                    selectedProposalForInfoSheet = selectedProposal
                                    showAiProposalEvaluationSheet = true
                                }
                            }
                        }
                    }

                    // Bottone Salva in basso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.applyReminderProposals(currentProposals) {
                                    isSaving = false
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                        ) {
                            Text(
                                "APPLICA MODIFICHE (${currentProposals.size})",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showAiProposalEvaluationSheet && selectedProposalForInfoSheet != null) {
            val proposal = selectedProposalForInfoSheet!!
            val isLocationEnabled by viewModel.locationEnabled.collectAsState()
            val city by viewModel.locationCity.collectAsState()
            
            val calendar = java.util.Calendar.getInstance()
            val month = calendar.get(java.util.Calendar.MONTH) // 0-11
            val season = when (month) {
                java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "Inverno"
                java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "Primavera"
                java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "Estate"
                else -> "Autunno"
            }

            ModalBottomSheet(
                onDismissRequest = { showAiProposalEvaluationSheet = false },
                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF141414) else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "VALUTAZIONE AI PROMEMORIA",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fattori considerati dal modello AI per proporre questo intervallo di promemoria:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AiProposalFactorItem("Pianta", proposal.plantName)
                    AiProposalFactorItem("Tipo di Promemoria", if (proposal.type == "WATERING") "ANNAFFIATURA" else "CONCIMAZIONE")
                    AiProposalFactorItem(
                        "Geolocalizzazione",
                        if (isLocationEnabled && city.isNotBlank()) "ATTIVA ($city)" else "NON ATTIVA"
                    )
                    AiProposalFactorItem("Stagionalità Corrente", season)
                    AiProposalFactorItem("Testo Sorgente Valutato", "\"${proposal.textSource.ifBlank { "Nessuna specifica" }}\"")
                    AiProposalFactorItem("Intervallo Suggerito", if (proposal.isDelete) "Sospensione" else "${proposal.daysInterval} giorni")
                    AiProposalFactorItem("Modello AI Utilizzato", "Gemini 3.5 Flash (Batch Optimizer)")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ProposalItemCard(
    proposal: ReminderProposal,
    onShowAiProposalEvaluation: (ReminderProposal) -> Unit
) {
    val activityType = if (proposal.type == "WATERING") "ANNAFFIATURA" else "CONCIMATURA"
    val activityColor = if (proposal.type == "WATERING") NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = proposal.plantName.uppercase(),
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activityType,
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = activityColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onShowAiProposalEvaluation(proposal) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Dettagli AI",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (proposal.isError) {
                Text(
                    text = proposal.errorText ?: "Impossibile generare promemoria",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Per risolvere, genera una nuova scheda di cura AI per questa pianta nella schermata di dettaglio o inserisci informazioni temporali più chiare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else if (proposal.isDelete) {
                Text(
                    text = "Azione consigliata: SOSPENDERE IL PROMEMORIA",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "La regola di cura dice: \"${proposal.textSource}\". Sospensione consigliata per questa stagione.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ATTUALE",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (proposal.currentDueDate != null) formatDate(proposal.currentDueDate) else "Nessuno",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROPOSTO (OGNI ${proposal.daysInterval} GG)",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingRed
                        )
                        Text(
                            text = formatDate(proposal.proposedDueDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = NothingFontFamily
                        )
                    }
                }

                if (proposal.textSource.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Regola: \"${proposal.textSource}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "-"
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun AiProposalFactorItem(label: String, value: String) {
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
