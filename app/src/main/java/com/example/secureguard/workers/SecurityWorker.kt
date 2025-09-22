package com.example.secureguard.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.secureguard.data.database.SecureGuardDatabase
import com.example.secureguard.data.repository.SecurityRepository

class SecurityWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val database = SecureGuardDatabase.getDatabase(applicationContext)
        val repository = SecurityRepository(applicationContext, database.threatDao(), database.whitelistDao())

        try {
            repository.refreshThreats()
            Log.d("SecurityWorker", "Periodic scan completed successfully.")
        } catch (e: Exception) {
            Log.e("SecurityWorker", "Periodic scan failed.", e)
            return Result.failure()
        }

        return Result.success()
    }
}

