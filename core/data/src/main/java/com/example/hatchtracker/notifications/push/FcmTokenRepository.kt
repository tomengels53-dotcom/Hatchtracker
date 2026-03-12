package com.example.hatchtracker.notifications.push

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import androidx.work.Data
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val prefs by lazy {
        withAllowedDiskReads {
            context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _currentToken = MutableStateFlow<String?>(readStringPref(KEY_TOKEN))
    val currentToken = _currentToken.asStateFlow()

    fun updateLocalToken(token: String) {
        val oldToken = readStringPref(KEY_TOKEN)
        if (oldToken != token) {
            writePrefs {
                putString(KEY_TOKEN, token)
            }
            _currentToken.value = token
            Logger.i(LogTags.NOTIFICATIONS, "op=updateLocalToken status=updated")
        }
    }

    fun requestTokenSyncAsync() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                updateLocalToken(token)
                enqueueUploadWork(token = token, uid = auth.currentUser?.uid, reason = "token_fetch_success")
            }
            .addOnFailureListener { e ->
                maybeLogFetchFailure(e.message)
                enqueueUploadWork(token = currentToken.value, uid = auth.currentUser?.uid, reason = "token_fetch_failed")
            }
    }

    suspend fun fetchTokenBestEffort(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    updateLocalToken(token)
                }
                if (continuation.isActive) continuation.resume(token)
            }
            .addOnFailureListener { e ->
                maybeLogFetchFailure(e.message)
                if (continuation.isActive) continuation.resume(null)
            }
    }

    fun onUserAuthenticated(uid: String?) {
        if (uid.isNullOrBlank()) return
        enqueueUploadWork(token = currentToken.value, uid = uid, reason = "user_authenticated")
    }

    private fun enqueueUploadWork(token: String?, uid: String?, reason: String) {
        val dataBuilder = Data.Builder()
        if (!token.isNullOrBlank()) dataBuilder.putString(FcmTokenUploadWorker.KEY_INPUT_TOKEN, token)
        if (!uid.isNullOrBlank()) dataBuilder.putString(FcmTokenUploadWorker.KEY_INPUT_UID, uid)
        FcmTokenUploadWorker.enqueue(context, dataBuilder.build())
        Logger.i(LogTags.NOTIFICATIONS, "op=enqueueFcmTokenUpload status=scheduled reason=$reason")
    }

    suspend fun uploadTokenIfPossible(inputToken: String?, inputUid: String?): UploadOutcome {
        if (!inputToken.isNullOrBlank()) {
            updateLocalToken(inputToken)
        }

        val uid = inputUid ?: auth.currentUser?.uid ?: return UploadOutcome.WAITING_FOR_LOGIN
        val token = inputToken ?: currentToken.value ?: fetchTokenBestEffort() ?: return UploadOutcome.RETRY

        val lastSyncToken = readStringPref(KEY_LAST_SYNC_TOKEN)
        val lastSyncTime = readLongPref(KEY_LAST_SYNC_TIME)
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L

        if (lastSyncToken == token && (now - lastSyncTime) < sevenDaysMs) {
            Logger.i(LogTags.NOTIFICATIONS, "op=uploadTokenIfPossible status=skipped reason=fresh")
            return UploadOutcome.SUCCESS
        }

        try {
            val tokenHash = sha256(token)
            val tokenData = mapOf(
                "token" to token,
                "platform" to "android",
                "createdAt" to Timestamp.now(),
                "lastSeenAt" to Timestamp.now(),
                "appVersion" to getAppVersion()
            )

            firestore.collection("users").document(uid)
                .collection("fcmTokens").document(tokenHash)
                .set(tokenData, SetOptions.merge())
                .await()

            writePrefs {
                putString(KEY_LAST_SYNC_TOKEN, token)
                putLong(KEY_LAST_SYNC_TIME, now)
                remove(KEY_LAST_PERMISSION_DENIED_AT)
            }

            Logger.i(LogTags.NOTIFICATIONS, "op=uploadTokenIfPossible status=success uid=$uid")
            return UploadOutcome.SUCCESS
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                writePrefs {
                    putLong(KEY_LAST_PERMISSION_DENIED_AT, now)
                }
            }
            maybeLogUploadFailure(e.message)
            return UploadOutcome.RETRY
        }
    }

    private fun maybeLogFetchFailure(message: String?) {
        val now = System.currentTimeMillis()
        val last = readLongPref(KEY_LAST_FETCH_FAILURE_LOG_TIME)
        if (now - last >= FAILURE_LOG_THROTTLE_MS) {
            writePrefs {
                putLong(KEY_LAST_FETCH_FAILURE_LOG_TIME, now)
            }
            Logger.e(LogTags.NOTIFICATIONS, "op=requestTokenSyncAsync status=failed error=$message")
        }
    }

    private fun maybeLogUploadFailure(message: String?) {
        val now = System.currentTimeMillis()
        val last = readLongPref(KEY_LAST_UPLOAD_FAILURE_LOG_TIME)
        if (now - last >= FAILURE_LOG_THROTTLE_MS) {
            writePrefs {
                putLong(KEY_LAST_UPLOAD_FAILURE_LOG_TIME, now)
            }
            Logger.e(LogTags.NOTIFICATIONS, "op=uploadTokenIfPossible status=failed error=$message")
        }
    }

    private fun readStringPref(key: String): String? = withAllowedDiskReads {
        prefs.getString(key, null)
    }

    private fun readLongPref(key: String): Long = withAllowedDiskReads {
        prefs.getLong(key, 0L)
    }

    private fun writePrefs(update: SharedPreferences.Editor.() -> Unit) {
        withAllowedDiskWrites {
            prefs.edit().apply(update).apply()
        }
    }

    private inline fun <T> withAllowedDiskReads(block: () -> T): T {
        val policy = StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(policy)
        }
    }

    private inline fun <T> withAllowedDiskWrites(block: () -> T): T {
        val policy = StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(policy)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        private const val KEY_TOKEN = "fcm_token"
        private const val KEY_LAST_SYNC_TOKEN = "last_sync_token"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_PERMISSION_DENIED_AT = "last_permission_denied_at"
        private const val KEY_LAST_FETCH_FAILURE_LOG_TIME = "last_fetch_failure_log_time"
        private const val KEY_LAST_UPLOAD_FAILURE_LOG_TIME = "last_upload_failure_log_time"
        private const val FAILURE_LOG_THROTTLE_MS = 60_000L
    }

    enum class UploadOutcome {
        SUCCESS,
        RETRY,
        WAITING_FOR_LOGIN
    }
}
