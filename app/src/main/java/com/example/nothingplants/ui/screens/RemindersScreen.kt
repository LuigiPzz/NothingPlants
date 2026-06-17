package com.example.nothingplants.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nothingplants.data.Reminder
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RemindersScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onNavigateToPlant: (Long) -> Unit
) {
    val activeReminders by viewModel.getActiveReminders().collectAsState(initial = emptyList())
    val completedReminders by viewModel.getCompletedReminders().collectAsState(initial = emptyList())
    val notificationHistory by viewModel.notificationHistory.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditMode by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedReminderForEdit by remember { mutableStateOf<Reminder?>(null) }

    val modifiedDates = remember { mutableStateMapOf<Long, Long>() }
    val deletedReminderIds = remember { mutableStateListOf<Long>() }

    val displayReminders = remember(activeReminders, completedReminders, isEditMode, modifiedDates, deletedReminderIds) {
        if (isEditMode) {
            (activeReminders + completedReminders)
                .filter { it.id !in deletedReminderIds }
                .map { reminder ->
                    if (reminder.id in modifiedDates) {
                        val newDueDate = modifiedDates[reminder.id]!!
                        reminder.copy(dueDate = newDueDate, isCompleted = newDueDate <= System.currentTimeMillis())
                    } else {
                        reminder
                    }
                }
        } else {
            activeReminders + completedReminders
        }
    }

    val sortedActive = displayReminders.filter { !it.isCompleted }.sortedBy { it.dueDate }
    val sortedCompleted = displayReminders.filter { it.isCompleted }.sortedByDescending { it.dueDate }

    BackHandler(enabled = isEditMode) {
        modifiedDates.clear()
        deletedReminderIds.clear()
        isEditMode = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) "Modifica Promemoria" else "Promemoria", 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            modifiedDates.clear()
                            deletedReminderIds.clear()
                            isEditMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (isEditMode) {
                        TextButton(onClick = {
                            viewModel.applyReminderEdits(
                                modifiedDates = modifiedDates.toMap(),
                                deletedIds = deletedReminderIds.toList()
                            ) {
                                modifiedDates.clear()
                                deletedReminderIds.clear()
                                isEditMode = false
                            }
                        }) {
                            Text(
                                "FINE",
                                fontFamily = NothingFontFamily,
                                color = NothingRed,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else if (selectedTab == 1 && notificationHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearNotificationHistory() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Cancella storico",
                                tint = MaterialTheme.colorScheme.onBackground
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
                .padding(horizontal = 16.dp)
        ) {
            if (!isEditMode) {
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
                                "DA FARE",
                                fontFamily = NothingFontFamily,
                                fontWeight = if (selectedTab == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
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
                                "STORICO NOTIFICHE",
                                fontFamily = NothingFontFamily,
                                fontWeight = if (selectedTab == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        selectedContentColor = NothingRed,
                        unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            if (selectedTab == 0 || isEditMode) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (sortedActive.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "DA FARE",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(sortedActive) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                plantName = "Pianta",
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onClick = {
                                    if (!isEditMode) {
                                        onNavigateToPlant(reminder.plantId)
                                    }
                                },
                                onLongClick = {
                                    isEditMode = true
                                },
                                onEditClick = {
                                    selectedReminderForEdit = reminder
                                    showDatePicker = true
                                },
                                onDeleteClick = {
                                    deletedReminderIds.add(reminder.id)
                                },
                                onComplete = { viewModel.toggleReminderStatus(reminder) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        }
                    } else if (sortedCompleted.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "NESSUN PROMEMORIA ATTIVO",
                                fontFamily = NothingFontFamily,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    
                    if (sortedCompleted.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "COMPLETATI",
                                fontFamily = NothingFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(sortedCompleted) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                plantName = "Pianta",
                                viewModel = viewModel,
                                isEditMode = isEditMode,
                                onClick = {
                                    if (!isEditMode) {
                                        onNavigateToPlant(reminder.plantId)
                                    }
                                },
                                onLongClick = {
                                    isEditMode = true
                                },
                                onEditClick = {
                                    selectedReminderForEdit = reminder
                                    showDatePicker = true
                                },
                                onDeleteClick = {
                                    deletedReminderIds.add(reminder.id)
                                },
                                onComplete = { viewModel.toggleReminderStatus(reminder) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    if (notificationHistory.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "NESSUNA NOTIFICA INVIATA",
                                fontFamily = NothingFontFamily,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        items(notificationHistory) { notification ->
                            NotificationHistoryItem(notification = notification)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker && selectedReminderForEdit != null) {
        val currentDueDate = modifiedDates[selectedReminderForEdit!!.id] ?: selectedReminderForEdit!!.dueDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentDueDate)
        DatePickerDialog(
            onDismissRequest = { 
                showDatePicker = false
                selectedReminderForEdit = null
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { newDate ->
                        modifiedDates[selectedReminderForEdit!!.id] = newDate
                    }
                    showDatePicker = false
                    selectedReminderForEdit = null
                }) {
                    Text("OK", fontFamily = NothingFontFamily, color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    selectedReminderForEdit = null
                }) {
                    Text("ANNULLA", fontFamily = NothingFontFamily, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderItem(
    reminder: Reminder,
    plantName: String,
    viewModel: PlantViewModel,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onComplete: () -> Unit
) {
    // Carichiamo il nome della pianta dinamicamente dal database (non bloccante)
    val plant by viewModel.getPlant(reminder.plantId).collectAsState(initial = null)
    val name = plant?.name?.takeIf { it.isNotBlank() } ?: plant?.species ?: plantName
    
    val isCompleted = reminder.isCompleted
    val isOverdue = !isCompleted && reminder.dueDate < System.currentTimeMillis()
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reminder.dueDate))
    
    val daysUntil = remember(reminder.dueDate, isOverdue) {
        val diffDays = com.example.nothingplants.utils.DateUtils.getDaysUntil(reminder.dueDate)
        if (isOverdue) {
            kotlin.math.abs(diffDays).toString()
        } else {
            if (diffDays < 0) "0" else diffDays.toString()
        }
    }

    val title = if (reminder.type == "WATERING") "Annaffiatura" else "Concimatura"
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete()
            },
            enabled = !isEditMode,
            modifier = Modifier.size(48.dp)
        ) {
            val categoryIcon = if (reminder.type == "WATERING") {
                if (isCompleted) Icons.Outlined.WaterDrop else Icons.Filled.WaterDrop
            } else {
                if (isCompleted) Icons.Outlined.Science else Icons.Filled.Science
            }
            Icon(
                imageVector = categoryIcon,
                contentDescription = if (isCompleted) "Ripristina" else "Completa",
                tint = when {
                    isCompleted -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    isOverdue -> NothingRed
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.uppercase(),
                fontFamily = NothingFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    isOverdue -> NothingRed
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Text(
                text = "$title - $date",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    isOverdue -> NothingRed
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                }
            )
        }

        if (isEditMode) {
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica data",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina promemoria",
                        tint = NothingRed
                    )
                }
            }
        } else if (!isCompleted) {
            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = daysUntil,
                    fontFamily = NothingFontFamily,
                    color = NothingRed,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (isOverdue) "RITARDO" else "GIORNI",
                    fontFamily = NothingFontFamily,
                    color = if (isOverdue) NothingRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun NotificationHistoryItem(
    notification: com.example.nothingplants.data.NotificationHistory
) {
    val dateStr = remember(notification.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICA INVIATA",
                    fontFamily = NothingFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingRed
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
