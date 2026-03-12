package com.example.hatchtracker.notifications.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.notifications.NotificationEngine
import com.example.hatchtracker.notifications.NotificationSeverity
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class PushIngressWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val engine: NotificationEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val payloadJson = inputData.getString(KEY_PAYLOAD_JSON) ?: return@withContext Result.failure()
        
        try {
            val envelope = Gson().fromJson(payloadJson, PushEnvelope::class.java)
            val event = engine.handleRemoteEvent(envelope)
            
            if (event != null) {
                NotificationHelper.showGenericNotification(
                    applicationContext,
                    event.incubationId,
                    event.title,
                    event.message,
                    isCritical = event.severity == NotificationSeverity.CRITICAL,
                    deeplink = event.deeplink
                )
            }
            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.NOTIFICATIONS, "op=PushIngressWorker status=failed payload=$payloadJson", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_PAYLOAD_JSON = "payload_json"
    }
}
