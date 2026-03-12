package com.example.hatchtracker.notifications.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FcmTokenUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val tokenRepository: FcmTokenRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_INPUT_TOKEN)
        val uid = inputData.getString(KEY_INPUT_UID)

        return when (tokenRepository.uploadTokenIfPossible(token, uid)) {
            FcmTokenRepository.UploadOutcome.SUCCESS -> Result.success()
            FcmTokenRepository.UploadOutcome.WAITING_FOR_LOGIN -> {
                Logger.i(LogTags.NOTIFICATIONS, "op=FcmTokenUploadWorker status=waiting_for_login")
                Result.success()
            }
            FcmTokenRepository.UploadOutcome.RETRY -> Result.retry()
        }
    }

    companion object {
        const val KEY_INPUT_TOKEN = "input_token"
        const val KEY_INPUT_UID = "input_uid"
        private const val UNIQUE_WORK_NAME = "fcm_token_upload_work"

        fun enqueue(context: Context, inputData: Data) {
            val request = OneTimeWorkRequestBuilder<FcmTokenUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
