package com.example.nothingplants.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingplants.data.InventoryItem
import com.example.nothingplants.data.ShoppingItem
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingAndInventoryScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Spesa & Inventario",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Selector in stile Nothing OS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = NothingRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NothingRed
                    )
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            "LISTA SPESA",
                            fontFamily = NothingFontFamily,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = NothingRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            "INVENTARIO",
                            fontFamily = NothingFontFamily,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = NothingRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> ShoppingListTab(viewModel)
                1 -> InventoryTab(viewModel)
            }
        }
    }
}

@Composable
fun ShoppingListTab(viewModel: PlantViewModel) {
    val shoppingItems by viewModel.allShoppingItems.collectAsStateWithLifecycle()
    var itemName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elenco spesa
            Box(modifier = Modifier.weight(1f)) {
                if (shoppingItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "LA LISTA DELLA SPESA È VUOTA",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shoppingItems, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item = item,
                                onToggle = { viewModel.toggleShoppingItem(item) },
                                onDelete = { viewModel.deleteShoppingItem(item) }
                            )
                        }

                        val hasPurchased = shoppingItems.any { it.isPurchased }
                        if (hasPurchased) {
                            item(key = "clear_purchased") {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.clearPurchasedItems()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingRed)
                                ) {
                                    Text("SVUOTA ACQUISTATI", fontFamily = NothingFontFamily)
                                }
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        } else {
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // FAB bianco in basso a destra
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddDialog = true
            },
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi articolo")
        }

        // Dialog per l'inserimento
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    itemName = ""
                },
                title = {
                    Text(
                        "NUOVO ARTICOLO",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Nome articolo", fontFamily = NothingFontFamily) },
                        placeholder = { Text("es. Terriccio, Vasi...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            focusedLabelColor = NothingRed,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                viewModel.addShoppingItem(itemName.trim(), 1)
                                itemName = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        enabled = itemName.isNotBlank()
                    ) {
                        Text("AGGIUNGI", fontFamily = NothingFontFamily, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showAddDialog = false
                            itemName = ""
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    ) {
                        Text("ANNULLA", fontFamily = NothingFontFamily)
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (item.isPurchased) 0.05f else 0.15f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Checkbox personalizzato in stile Nothing OS
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (item.isPurchased) NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isPurchased) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.isPurchased) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete()
        }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Elimina articolo",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryTab(viewModel: PlantViewModel) {
    val inventoryItems by viewModel.allInventoryItems.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("Concime") }  // "Concime" o "Terreno"
    var levelState by remember { mutableFloatStateOf(100f) }
    var showAddForm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Stato per il dialog di aggiorna quantità
    var itemToUpdate by remember { mutableStateOf<com.example.nothingplants.data.InventoryItem?>(null) }
    var updateSliderLevel by remember { mutableFloatStateOf(50f) }

    // Dialog: Aggiorna quantità con Slider
    itemToUpdate?.let { item ->
        // Inizializza lo slider al livello corrente quando si apre
        LaunchedEffect(item.id) {
            updateSliderLevel = item.levelPercent.toFloat()
        }

        AlertDialog(
            onDismissRequest = { itemToUpdate = null },
            title = {
                Text(
                    "AGGIORNA LIVELLO",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        item.name.uppercase(),
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Column {
                        Text(
                            text = "LIVELLO: ${updateSliderLevel.toInt()}%",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Slider(
                            value = updateSliderLevel,
                            onValueChange = { updateSliderLevel = it },
                            valueRange = 0f..100f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = NothingRed,
                                activeTrackColor = NothingRed,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateInventoryItemLevel(item, updateSliderLevel.toInt())
                        itemToUpdate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text("SALVA", fontFamily = NothingFontFamily, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToUpdate = null },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                ) {
                    Text("ANNULLA", fontFamily = NothingFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showAddForm) {
                // Form di aggiunta concime
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "AGGIUNGI ALL'INVENTARIO",
                        fontFamily = NothingFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        color = NothingRed
                    )

                    // Selettore tipo: Concime / Terreno
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NothingRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        listOf("Concime", "Terreno").forEachIndexed { index, label ->
                            val isSelected = itemType == label
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NothingRed else Color.Transparent
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        itemType = label
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.uppercase(),
                                    fontFamily = NothingFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            if (index == 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(NothingRed.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome", fontFamily = NothingFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            focusedLabelColor = NothingRed,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Column {
                        Text(
                            text = "LIVELLO INIZIALE: ${levelState.toInt()}%",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Slider(
                            value = levelState,
                            onValueChange = { levelState = it },
                            valueRange = 0f..100f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = NothingRed,
                                activeTrackColor = NothingRed,
                                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAddForm = false
                                name = ""
                                itemType = "Concime"
                                levelState = 100f
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ANNULLA", fontFamily = NothingFontFamily)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (name.isNotBlank()) {
                                    viewModel.addInventoryItem("[$itemType] ${name.trim()}", levelState.toInt(), "")
                                    name = ""
                                    itemType = "Concime"
                                    levelState = 100f
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                            enabled = name.isNotBlank()
                        ) {
                            Text("SALVA", fontFamily = NothingFontFamily, color = Color.White)
                        }
                    }
                }
            }

            // Griglia inventario 3 colonne
            Box(modifier = Modifier.weight(1f)) {
                if (inventoryItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "INVENTARIO VUOTO",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(inventoryItems, key = { it.id }) { item ->
                            InventoryGridCell(
                                item = item,
                                onDelete = { viewModel.deleteInventoryItem(item) },
                                onUpdateLevel = {
                                    itemToUpdate = item
                                }
                            )
                        }
                    }
                }
            }
        }

        // FAB bianco in basso a destra (solo se il form non è mostrato)
        if (!showAddForm) {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddForm = true
                },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi inventario")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryGridCell(
    item: com.example.nothingplants.data.InventoryItem,
    onDelete: () -> Unit,
    onUpdateLevel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .border(
                width = 0.5.dp,
                color = if (item.levelPercent <= 15)
                    NothingRed.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            val isSoil = item.name.startsWith("[Terreno]")
            val displayName = item.name
                .replace("[Terreno]", "")
                .replace("[Concime]", "")
                .trim()

            // Icona animata (sacchetto o bottiglia)
            if (isSoil) {
                SoilBagIcon(
                    levelPercent = item.levelPercent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                FertilizerBottleIcon(
                    levelPercent = item.levelPercent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            // Nome concime / terreno pulito
            Text(
                text = displayName.uppercase(),
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = androidx.compose.ui.unit.TextUnit(
                    14f,
                    androidx.compose.ui.unit.TextUnitType.Sp
                )
            )
        }

        // DropdownMenu contestuale (long press)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Aggiorna quantità",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    onUpdateLevel()
                }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = NothingRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Elimina",
                            fontFamily = NothingFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NothingRed
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun FertilizerBottleIcon(
    levelPercent: Int,
    modifier: Modifier = Modifier
) {
    // Animazione fluida del livello quando cambia
    val animLevel by animateFloatAsState(
        targetValue = levelPercent.toFloat() / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "liquidLevel"
    )

    // Animazione infinita per l'onda del liquido
    val infiniteTransition = rememberInfiniteTransition(label = "liquidWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val onBgColor = MaterialTheme.colorScheme.onBackground
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Definiamo la silhouette interna per il ritaglio (clipPath)
            val bottleInnerPath = Path().apply {
                // Iniziamo dal collo in alto a sinistra
                moveTo(w * 0.42f, h * 0.15f)
                lineTo(w * 0.42f, h * 0.28f)
                // Spalla sinistra
                quadraticTo(w * 0.42f, h * 0.36f, w * 0.18f, h * 0.38f)
                // Lato sinistro
                lineTo(w * 0.18f, h * 0.88f)
                // Angolo inferiore sinistro
                quadraticTo(w * 0.18f, h * 0.94f, w * 0.30f, h * 0.94f)
                // Fondo
                lineTo(w * 0.70f, h * 0.94f)
                // Angolo inferiore destro
                quadraticTo(w * 0.82f, h * 0.94f, w * 0.82f, h * 0.88f)
                // Lato destro
                lineTo(w * 0.82f, h * 0.38f)
                // Spalla destra
                quadraticTo(w * 0.58f, h * 0.36f, w * 0.58f, h * 0.28f)
                // Collo in alto a destra
                lineTo(w * 0.58f, h * 0.15f)
                close()
            }

            // 2. Disegniamo il liquido all'interno dell'area ritagliata
            if (animLevel > 0f) {
                clipPath(bottleInnerPath) {
                    val usableMinY = h * 0.94f
                    val usableMaxY = h * 0.18f
                    val liquidY = usableMinY - (usableMinY - usableMaxY) * animLevel

                    val liquidPath = Path().apply {
                        moveTo(-10f, h + 10f)
                        lineTo(-10f, liquidY)

                        // Calcoliamo l'onda sinusoidale
                        val waveAmplitude = h * 0.025f
                        val waveFrequency = 1.2f
                        val steps = 30
                        for (i in 0..steps) {
                            val xCoord = w * (i.toFloat() / steps)
                            val relativeX = i.toFloat() / steps
                            val waveY = liquidY + waveAmplitude * kotlin.math.sin(relativeX * waveFrequency * 2 * Math.PI + wavePhase)
                            lineTo(xCoord, waveY.toFloat())
                        }

                        lineTo(w + 10f, liquidY)
                        lineTo(w + 10f, h + 10f)
                        close()
                    }

                    // Riempimento liquido con gradiente NothingRed
                    drawPath(
                        path = liquidPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NothingRed, NothingRed.copy(alpha = 0.5f)),
                            startY = liquidY,
                            endY = h
                        )
                    )
                }
            }

            // 3. Disegniamo la silhouette esterna (vetro della bottiglia)
            val bottleOuterPath = Path().apply {
                moveTo(w * 0.40f, h * 0.14f)
                lineTo(w * 0.40f, h * 0.28f)
                quadraticTo(w * 0.40f, h * 0.37f, w * 0.16f, h * 0.39f)
                lineTo(w * 0.16f, h * 0.88f)
                quadraticTo(w * 0.16f, h * 0.96f, w * 0.28f, h * 0.96f)
                lineTo(w * 0.72f, h * 0.96f)
                quadraticTo(w * 0.84f, h * 0.96f, w * 0.84f, h * 0.88f)
                lineTo(w * 0.84f, h * 0.39f)
                quadraticTo(w * 0.60f, h * 0.37f, w * 0.60f, h * 0.28f)
                lineTo(w * 0.60f, h * 0.14f)
                close()
            }

            drawPath(
                path = bottleOuterPath,
                color = onBgColor.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Tappo della bottiglia
            drawRoundRect(
                color = onBgColor,
                topLeft = Offset(w * 0.34f, h * 0.04f),
                size = Size(w * 0.32f, h * 0.10f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Testo percentuale sovrapposto al centro del corpo della bottiglia
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 22.dp)
        ) {
            Text(
                text = "${levelPercent}%",
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = if (levelPercent >= 50) Color.White else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
