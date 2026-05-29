package com.example.nothingplants.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nothingplants.data.SyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account == null) {
                return Result.success()
            }

            val syncRepository = SyncRepository(applicationContext)
            val result = syncRepository.upload(account)
            
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
