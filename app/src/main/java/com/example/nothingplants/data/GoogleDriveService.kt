package com.example.nothingplants.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class GoogleDriveService(private val context: Context) {
    private val driveScope = DriveScopes.DRIVE_APPDATA

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(driveScope))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(driveScope))
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Nothing Plants").build()
    }

    suspend fun uploadSyncFile(account: GoogleSignInAccount, jsonContent: String): String = withContext(Dispatchers.IO) {
        val drive = getDriveService(account)
        
        val query = "name = 'nothing_plants_backup.json' and 'appDataFolder' in parents"
        val fileList = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .execute()
        
        val metadata = com.google.api.services.drive.model.File().apply {
            name = "nothing_plants_backup.json"
            parents = listOf("appDataFolder")
        }
        
        val content = ByteArrayContent.fromString("application/json", jsonContent)
        
        if (fileList.files.isEmpty()) {
            val createdFile = drive.files().create(metadata, content).execute()
            createdFile.id
        } else {
            val fileId = fileList.files[0].id
            drive.files().update(fileId, null, content).execute()
            fileId
        }
    }

    suspend fun downloadSyncFile(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val drive = getDriveService(account)
        
        val query = "name = 'nothing_plants_backup.json' and 'appDataFolder' in parents"
        val fileList = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .execute()
        
        if (fileList.files.isEmpty()) return@withContext null
        
        val fileId = fileList.files[0].id
        val outputStream = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.toString("UTF-8")
    }

    suspend fun uploadFile(account: GoogleSignInAccount, file: File, mimeType: String): String = withContext(Dispatchers.IO) {
        val drive = getDriveService(account)
        
        val query = "name = '${file.name}' and 'appDataFolder' in parents"
        val fileList = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .execute()
            
        val metadata = com.google.api.services.drive.model.File().apply {
            name = file.name
            parents = listOf("appDataFolder")
        }
        
        val content = FileContent(mimeType, file)
        
        if (fileList.files.isEmpty()) {
            val createdFile = drive.files().create(metadata, content).execute()
            createdFile.id
        } else {
            fileList.files[0].id
        }
    }

    suspend fun cleanupObsoletePhotos(account: GoogleSignInAccount, validFilenames: Set<String>) = withContext(Dispatchers.IO) {
        val drive = getDriveService(account)
        var pageToken: String? = null
        do {
            val result = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name != 'nothing_plants_backup.json'")
                .setPageToken(pageToken)
                .execute()
                
            for (file in result.files) {
                if (!validFilenames.contains(file.name)) {
                    try {
                        drive.files().delete(file.id).execute()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
    }

    suspend fun downloadFile(account: GoogleSignInAccount, fileId: String, destFile: File) = withContext(Dispatchers.IO) {
        val drive = getDriveService(account)
        destFile.parentFile?.mkdirs()
        destFile.outputStream().use { outputStream ->
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        }
    }
}
