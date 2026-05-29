package com.example.nothingplants

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import com.example.nothingplants.ui.screens.AddPlantScreen
import com.example.nothingplants.ui.screens.HomeScreen
import com.example.nothingplants.ui.screens.PlantDetailScreen
import com.example.nothingplants.ui.theme.NothingPlantsTheme
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingplants.ui.PlantViewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.nothingplants.worker.ReminderWorker
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

sealed class Screen {
    object Home : Screen()
    data class AddPlant(val plantId: Long? = null) : Screen()
    data class PlantDetail(val plantId: Long) : Screen()
    data class History(val plantId: Long) : Screen()
    data class GrowthDiary(val plantId: Long) : Screen()
    object Reminders : Screen()
    object Settings : Screen()
    object ReminderVerification : Screen()
    object FertilizerSummary : Screen()
    object SoilSummary : Screen()
    object PlantHealthCheck : Screen()
    object ShoppingAndInventory : Screen()
}

class MainActivity : ComponentActivity() {
    private val viewModel: PlantViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Gesti permesso
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Calcoliamo il tempo rimanente fino alle 09:00 AM di oggi o di domani
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - now

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        enableEdgeToEdge()
        setContent {
            NothingPlantsTheme {
                val screenStack = remember { mutableStateListOf<Screen>(Screen.Home) }
                val currentScreen = screenStack.lastOrNull() ?: Screen.Home
                
                BackHandler(enabled = screenStack.size > 1) {
                    screenStack.removeLast()
                }

                when (val screen = currentScreen) {
                    is Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { screenStack.add(Screen.AddPlant()) },
                        onNavigateToDetail = { id -> screenStack.add(Screen.PlantDetail(id)) },
                        onNavigateToSettings = { screenStack.add(Screen.Settings) },
                        onNavigateToReminders = { screenStack.add(Screen.Reminders) },
                        onNavigateToHealthCheck = { screenStack.add(Screen.PlantHealthCheck) },
                        onNavigateToShopping = { screenStack.add(Screen.ShoppingAndInventory) },
                        onNavigateToFertilizerSummary = { screenStack.add(Screen.FertilizerSummary) },
                        onNavigateToSoilSummary = { screenStack.add(Screen.SoilSummary) }
                    )
                    is Screen.AddPlant -> AddPlantScreen(
                        plantId = screen.plantId,
                        viewModel = viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home }
                    )
                    is Screen.PlantDetail -> PlantDetailScreen(
                        plantId = screen.plantId,
                        viewModel = viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home },
                        onEdit = { screenStack.add(Screen.AddPlant(screen.plantId)) },
                        onNavigateToHistory = { screenStack.add(Screen.History(screen.plantId)) }
                    )
                    is Screen.History -> com.example.nothingplants.ui.screens.HistoryScreen(
                        plantId = screen.plantId,
                        viewModel = viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.PlantDetail(screen.plantId) },
                        onNavigateToGrowthDiary = { id -> screenStack.add(Screen.GrowthDiary(id)) }
                    )
                    is Screen.GrowthDiary -> com.example.nothingplants.ui.screens.GrowthDiaryScreen(
                        plantId = screen.plantId,
                        viewModel = viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.PlantDetail(screen.plantId) }
                    )
                    is Screen.Reminders -> com.example.nothingplants.ui.screens.RemindersScreen(
                        viewModel = viewModel,
                        onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home },
                        onNavigateToPlant = { id -> screenStack.add(Screen.PlantDetail(id)) }
                    )
                    is Screen.Settings -> {
                        com.example.nothingplants.ui.screens.SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToVerification = { screenStack.add(Screen.ReminderVerification) },
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home }
                        )
                    }
                    is Screen.ReminderVerification -> {
                        com.example.nothingplants.ui.screens.ReminderVerificationScreen(
                            viewModel = viewModel,
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Settings }
                        )
                    }
                    is Screen.FertilizerSummary -> {
                        com.example.nothingplants.ui.screens.FertilizerSummaryScreen(
                            viewModel = viewModel,
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Settings }
                        )
                    }
                    is Screen.SoilSummary -> {
                        com.example.nothingplants.ui.screens.SoilSummaryScreen(
                            viewModel = viewModel,
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Settings }
                        )
                    }
                    is Screen.PlantHealthCheck -> {
                        com.example.nothingplants.ui.screens.PlantHealthCheckScreen(
                            viewModel = viewModel,
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home }
                        )
                    }
                    is Screen.ShoppingAndInventory -> {
                        com.example.nothingplants.ui.screens.ShoppingAndInventoryScreen(
                            viewModel = viewModel,
                            onBack = { if (screenStack.size > 1) screenStack.removeLast() else screenStack[0] = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}
