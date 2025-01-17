package com.example.geofencinginitialdemo.old

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class GeofenceExitWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        com.example.geofencinginitialdemo.permission.sendNotification(context = this.applicationContext)
        return Result.success()
    }

    private fun sendNotification() {
        // Create and show notification here
    }

    companion object {
        fun scheduleExitWorker(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<GeofenceExitWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
