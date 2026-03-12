package com.example.hatchtracker.core.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import java.io.File

/**
 * Handles profile image persistence, sampling, and cleanup.
 */
@Singleton
class ProfileImageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Saves a profile photo from a URI to internal storage.
     * Handles sampling and compression off the main thread.
     * Returns the absolute path if successful, or null on failure.
     */
    suspend fun saveProfilePhoto(uri: Uri): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val savedPath = saveImageToInternalStorage(uri)
            Result.success(savedPath)
        } catch (e: Exception) {
            Logger.e(LogTags.SYNC, "Error saving profile photo", e)
            Result.failure(e)
        }
    }

    /**
     * Safely deletes an old profile photo file.
     */
    fun deleteOldPhoto(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Logger.e(LogTags.SYNC, "Error deleting old photo: $path", e)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        return inputStream.use { input ->
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.filesDir, fileName)
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
            outputFile.absolutePath
        }
    }
}
