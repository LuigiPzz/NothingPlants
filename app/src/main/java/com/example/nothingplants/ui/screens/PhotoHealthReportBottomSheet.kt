package com.example.nothingplants.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoHealthReportBottomSheet(
    state: PlantViewModel.PhotoHealthState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (state) {
                is PlantViewModel.PhotoHealthState.Idle -> {
                    // Nulla
                }
                is PlantViewModel.PhotoHealthState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NothingRed)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "ANALISI FOTO IN CORSO...",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "L'AI sta analizzando lo stato visivo di foglie, fusti e terra per individuare quanti più elementi possibili.",
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
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "ERRORE DI ANALISI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            color = NothingRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                is PlantViewModel.PhotoHealthState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "DIAGNOSI SALUTE AI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            color = NothingRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 450.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            MarkdownText(
                                text = state.report,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "CHIUDI",
                                fontFamily = NothingFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("1. **") || trimmed.startsWith("2. **") || trimmed.startsWith("3. **") || trimmed.startsWith("4. **") || trimmed.startsWith("###") || (trimmed.startsWith("**") && trimmed.endsWith("**")) -> {
                    val cleaned = trimmed
                        .replace("###", "")
                        .replace("**", "")
                        .replace(Regex("^\\d+\\.\\s*"), "")
                        .trim()
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cleaned.uppercase(),
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        color = NothingRed
                    )
                }
                trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val cleaned = trimmed.substring(1).trim()
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = NothingRed, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = parseBoldText(cleaned),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color.copy(alpha = 0.85f)
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = parseBoldText(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
