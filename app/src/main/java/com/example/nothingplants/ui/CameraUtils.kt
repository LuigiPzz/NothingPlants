package com.example.nothingplants.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraUtils {
    fun createTempImageUri(context: Context): Uri {
        val imageFile = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val file = File(imageFile, "temp_image_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}
