package com.example.nothingplants.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.FileOutputStream
import kotlin.math.min

class ImageProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)

object ImageProcessor {

    suspend fun processAndSaveSquareVibrant(context: Context, uri: Uri, destPath: String): Boolean {
        try {
            val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                ?: throw ImageProcessingException("Impossibile caricare l'immagine originale.")
            
            val squareSize = min(originalBitmap.width, originalBitmap.height)
            val startX = (originalBitmap.width - squareSize) / 2
            val startY = (originalBitmap.height - squareSize) / 2
            val squareBitmap = Bitmap.createBitmap(originalBitmap, startX, startY, squareSize, squareSize)

            FileOutputStream(destPath).use { out ->
                squareBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            return true
        } catch (e: Exception) {
            throw ImageProcessingException("Impossibile completare l'elaborazione dell'immagine: ${e.localizedMessage}", e)
        }
    }
}
