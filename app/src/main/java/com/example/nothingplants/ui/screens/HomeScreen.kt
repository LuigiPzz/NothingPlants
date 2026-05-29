package com.example.nothingplants.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingplants.data.Plant
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToHealthCheck: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToFertilizerSummary: () -> Unit,
    onNavigateToSoilSummary: () -> Unit
) {
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val groupedPlants = remember(plants) {
        plants.groupBy { it.room?.trim()?.uppercase() ?: "" }
            .toSortedMap { room1, room2 ->
                when {
                    room1.isBlank() && room2.isBlank() -> 0
                    room1.isBlank() -> 1  // Senza posizione va alla fine
                    room2.isBlank() -> -1 // Senza posizione va alla fine
                    else -> room1.compareTo(room2)
                }
            }
    }

    var showBottomSheetMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Plant")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (showBottomSheetMenu) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheetMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "MENU",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToShopping()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "SPESA & INVENTARIO",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToHealthCheck()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "VERIFICA SALUTE AI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToReminders()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "PROMEMORIA",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToFertilizerSummary()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "RIEPILOGO CONCIMI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToSoilSummary()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "RIEPILOGO TERRENI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBottomSheetMenu = false
                                onNavigateToSettings()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "IMPOSTAZIONI",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Row (Nothing news style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    // ── TITOLO ──────────────────────────────────────────────────
                    Text(
                        text = "Nothing Plants",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline()
                    )

                    // ── SIMBOLO + NUMERO ────────────────────────────────────────
                    Text(
                        text = " · ${plants.size}",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 26.sp),
                        color = NothingRed,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                
                IconButton(onClick = { showBottomSheetMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu opzioni",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item(key = "sort_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ORDINA PER",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { viewModel.setSortOrder("ROOM") },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sortOrder == "ROOM") {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                )
                            }
                            Text(
                                text = "STANZA",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sortOrder == "ROOM") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.clickable { viewModel.setSortOrder("WATERING") },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sortOrder == "WATERING") {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                )
                            }
                            Text(
                                text = "ANNAFFIATURA",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sortOrder == "WATERING") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            if (sortOrder == "ROOM") {
                groupedPlants.forEach { (room, roomPlants) ->
                    item(key = "header_$room") {
                        val headerText = if (room.isBlank()) "SENZA POSIZIONE" else room
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = headerText,
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = NothingRed
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                            )
                        }
                    }
                    
                    items(roomPlants, key = { it.id }) { plant ->
                        PlantCard(
                            plant = plant,
                            viewModel = viewModel,
                            showRoom = false,
                            onClick = { onNavigateToDetail(plant.id) }
                        )
                    }
                }
            } else {
                items(plants, key = { it.id }) { plant ->
                    PlantCard(
                        plant = plant,
                        viewModel = viewModel,
                        showRoom = true,
                        onClick = { onNavigateToDetail(plant.id) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
}

@Composable
fun PlantCard(
    plant: Plant,
    viewModel: PlantViewModel,
    showRoom: Boolean,
    onClick: () -> Unit
) {
    val activeReminder by viewModel.getActiveWateringReminderForPlant(plant.id).collectAsStateWithLifecycle(initialValue = null)
    
    val daysUntilWatering = remember(activeReminder) {
        val reminder = activeReminder
        if (reminder == null) {
            "?"
        } else {
            val diffMs = reminder.dueDate - System.currentTimeMillis()
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays < 0) "0" else diffDays.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayName = if (plant.name.isNotBlank()) plant.name.uppercase() else plant.species.uppercase()
                Text(
                    text = displayName,
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (plant.name.isNotBlank() && plant.species.isNotBlank()) {
                    Text(
                        text = plant.species,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                if (showRoom && !plant.room.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plant.room.uppercase(),
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = daysUntilWatering,
                    fontFamily = NothingFontFamily,
                    color = NothingRed,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "GIORNI",
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
