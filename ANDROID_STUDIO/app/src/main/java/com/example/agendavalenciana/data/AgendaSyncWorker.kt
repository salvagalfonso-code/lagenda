package com.example.agendavalenciana.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AgendaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        if (!RemoteAgendaRepository.isConfigured()) return Result.success()
        return RemoteAgendaRepository.sync(applicationContext).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

object AgendaSyncScheduler {
    private const val UNIQUE_WORK = "agenda_valenciana_remote_sync"

    fun schedule(context: Context) {
        if (!RemoteAgendaRepository.isConfigured()) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = PeriodicWorkRequestBuilder<AgendaSyncWorker>(4, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
}
