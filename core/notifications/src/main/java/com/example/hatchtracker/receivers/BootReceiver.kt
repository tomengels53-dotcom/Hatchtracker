package com.example.hatchtracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.SafeRun
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.notifications.scheduling.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun database(): AppDatabase
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                var activeCount = 0
                val result = SafeRun.runSuspend(LogTags.NOTIFICATIONS, "BootReceiver#onReceive") {
                    val database = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        BootReceiverEntryPoint::class.java
                    ).database()
                    val incubationDao = database.incubationDao()
                    
                    // Fetch active incubations
                    val activeIncubations = incubationDao.getAllIncubationEntitys().filter { !it.hatchCompleted }
                    activeCount = activeIncubations.size
                    
                    NotificationScheduler.rescheduleAll(context, activeIncubations)
                }

                result.onFailure { throwable ->
                    Logger.e(
                        LogTags.NOTIFICATIONS,
                        "op=BootReceiver#onReceive activeIncubations=$activeCount",
                        throwable
                    )
                }
            }
        }
    }
}
