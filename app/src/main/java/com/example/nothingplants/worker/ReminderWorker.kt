package com.example.nothingplants.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nothingplants.MainActivity
import com.example.nothingplants.data.AppDatabase
import com.example.nothingplants.R
import java.util.Calendar

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val reminderDao = database.reminderDao()
        val plantDao = database.plantDao()

        // Controlla i promemoria scaduti (fino alla fine di oggi)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endOfDay = calendar.timeInMillis

        val dueReminders = reminderDao.getDueRemindersSync(endOfDay).toMutableList()

        if (dueReminders.isNotEmpty()) {
            val plantIdsWithWatering = dueReminders.filter { it.type == "WATERING" }.map { it.plantId }.toSet()
            val plantIdsWithFertilizing = dueReminders.filter { it.type == "FERTILIZING" }.map { it.plantId }.toSet()
            val threeDaysMs = 3 * 24L * 60 * 60 * 1000L
            val additionalReminders = mutableListOf<com.example.nothingplants.data.Reminder>()

            plantIdsWithWatering.forEach { plantId ->
                if (!plantIdsWithFertilizing.contains(plantId)) {
                    val activeFertilizing = reminderDao.getActiveReminderForPlantAndTypeSync(plantId, "FERTILIZING")
                    if (activeFertilizing != null) {
                        val wateringRem = dueReminders.first { it.plantId == plantId && it.type == "WATERING" }
                        if (Math.abs(activeFertilizing.dueDate - wateringRem.dueDate) <= threeDaysMs) {
                            additionalReminders.add(activeFertilizing)
                        }
                    }
                }
            }

            plantIdsWithFertilizing.forEach { plantId ->
                if (!plantIdsWithWatering.contains(plantId)) {
                    val activeWatering = reminderDao.getActiveReminderForPlantAndTypeSync(plantId, "WATERING")
                    if (activeWatering != null) {
                        val fertilizingRem = dueReminders.first { it.plantId == plantId && it.type == "FERTILIZING" }
                        if (Math.abs(activeWatering.dueDate - fertilizingRem.dueDate) <= threeDaysMs) {
                            additionalReminders.add(activeWatering)
                        }
                    }
                }
            }

            dueReminders.addAll(additionalReminders)
        }

        if (dueReminders.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return Result.success() // Non possiamo mostrare notifiche
                }
            }

            createNotificationChannel()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Possiamo passare un extra per aprire direttamente i promemoria
                putExtra("OPEN_REMINDERS", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val wateringReminders = dueReminders.filter { it.type == "WATERING" }
            val fertilizingReminders = dueReminders.filter { it.type == "FERTILIZING" }

            val wateringPlantIds = wateringReminders.map { it.plantId }.toSet()
            val fertilizingPlantIds = fertilizingReminders.map { it.plantId }.toSet()

            val bothIds = wateringPlantIds.intersect(fertilizingPlantIds)
            val onlyWateringIds = wateringPlantIds - bothIds
            val onlyFertilizingIds = fertilizingPlantIds - bothIds

            suspend fun getPlantName(id: Long): String {
                val plant = plantDao.getPlantByIdSync(id)
                return plant?.let { if (it.name.isNotBlank()) it.name else it.species } ?: "Sconosciuta"
            }

            val bothPlants = bothIds.map { getPlantName(it) }.distinct()
            val onlyWateringPlants = onlyWateringIds.map { getPlantName(it) }.distinct()
            val onlyFertilizingPlants = onlyFertilizingIds.map { getPlantName(it) }.distinct()

            val textParts = mutableListOf<String>()
            if (bothPlants.isNotEmpty()) {
                textParts.add("Annaffiare e concimare: ${bothPlants.joinToString(", ")}")
            }
            if (onlyWateringPlants.isNotEmpty()) {
                textParts.add("Annaffiare: ${onlyWateringPlants.joinToString(", ")}")
            }
            if (onlyFertilizingPlants.isNotEmpty()) {
                textParts.add("Concimare: ${onlyFertilizingPlants.joinToString(", ")}")
            }

            val text = if (textParts.isEmpty()) {
                "Le tue piante hanno bisogno di cure oggi!"
            } else {
                textParts.joinToString(" | ")
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nothing Plants")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Salva lo storico delle notifiche nel database
            val notificationLog = com.example.nothingplants.data.NotificationHistory(
                message = text,
                timestamp = System.currentTimeMillis()
            )
            reminderDao.insertNotificationHistory(notificationLog)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Promemoria Piante"
            val descriptionText = "Notifiche per annaffiature e concimature"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "nothing_plants_reminders"
        const val NOTIFICATION_ID = 1001
    }
}
