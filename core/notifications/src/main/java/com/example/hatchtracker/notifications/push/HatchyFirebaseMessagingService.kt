package com.example.hatchtracker.notifications.push

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.workDataOf
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.notifications.push.PushIngressWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class HatchyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.i(LogTags.NOTIFICATIONS, "op=onNewToken status=received")
        applicationContext
            .getSharedPreferences("fcm_token_prefs", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()

        FcmTokenUploadWorker.enqueue(
            context = applicationContext,
            inputData = Data.Builder()
                .putString(FcmTokenUploadWorker.KEY_INPUT_TOKEN, token)
                .build()
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.i(LogTags.NOTIFICATIONS, "op=onMessageReceived status=received from=${message.from}")

        val data = message.data
        if (data.isNotEmpty()) {
            val payloadJson = Gson().toJson(data)
            Logger.d(LogTags.NOTIFICATIONS, "op=onMessageReceived payload=$payloadJson")

            val workRequest = OneTimeWorkRequestBuilder<PushIngressWorker>()
                .setInputData(workDataOf(PushIngressWorker.KEY_PAYLOAD_JSON to payloadJson))
                .addTag("push_ingress_${System.currentTimeMillis()}")
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)
        } else {
            Logger.w(LogTags.NOTIFICATIONS, "op=onMessageReceived status=skipped reason=empty_data")
        }
    }
}
