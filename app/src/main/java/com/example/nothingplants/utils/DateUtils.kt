package com.example.nothingplants.utils

import java.util.Calendar

object DateUtils {
    fun getDaysUntil(dueDateMs: Long): Int {
        val calDue = Calendar.getInstance().apply { 
            timeInMillis = dueDateMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calNow = Calendar.getInstance().apply { 
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = calDue.timeInMillis - calNow.timeInMillis
        return Math.round(diffMs.toDouble() / (1000L * 60 * 60 * 24)).toInt()
    }
}
