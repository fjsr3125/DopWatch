package com.sora.dopwatch

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sora.dopwatch.api.LocalApiServer
import com.sora.dopwatch.worker.UsageCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DopWatchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var localApiServer: LocalApiServer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleUsageCheck()
        startApiServer()
    }

    private fun startApiServer() {
        try {
            localApiServer.start()
            Log.i("DopWatch", "Local API server started on port 8080")
        } catch (e: Exception) {
            Log.e("DopWatch", "Failed to start API server", e)
        }
    }

    private fun scheduleUsageCheck() {
        val workRequest = PeriodicWorkRequestBuilder<UsageCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "usage_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
