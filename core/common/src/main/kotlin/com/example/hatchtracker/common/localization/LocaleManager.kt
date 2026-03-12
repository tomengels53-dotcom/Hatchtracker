package com.example.hatchtracker.common.localization

import android.app.LocaleManager as FrameworkLocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Applies the specified language tag to the application.
     * Use empty string or null for system default.
     */
    fun applyLanguage(tag: String?) {
        val effectiveTag = tag ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val frameworkLocaleManager = context.getSystemService(FrameworkLocaleManager::class.java)
            val currentTags = frameworkLocaleManager?.applicationLocales?.toLanguageTags() ?: ""
            if (currentTags == effectiveTag) {
                android.util.Log.d("LocaleManager", "op=applyLanguage status=skipped reason=already_applied tag='$effectiveTag'")
                return
            }
            android.util.Log.d("LocaleManager", "op=applyLanguage status=requesting tag='$effectiveTag'")

            frameworkLocaleManager?.applicationLocales =
                if (effectiveTag.isBlank()) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(effectiveTag)

            val appliedTags = frameworkLocaleManager?.applicationLocales?.toLanguageTags() ?: ""
            android.util.Log.d("LocaleManager", "op=applyLanguage status=success applied='$appliedTags'")
            return
        }

        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags == effectiveTag) {
            android.util.Log.d("LocaleManager", "op=applyLanguage status=skipped reason=already_applied tag='$effectiveTag'")
            return
        }
        android.util.Log.d("LocaleManager", "op=applyLanguage status=requesting tag='$effectiveTag'")
        
        val appLocales = if (effectiveTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(effectiveTag)
        }
        
        AppCompatDelegate.setApplicationLocales(appLocales)
        
        val appliedTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        android.util.Log.d("LocaleManager", "op=applyLanguage status=success applied='$appliedTags'")
    }

    /**
     * Gets the current application locale tag.
     */
    fun getCurrentLanguageTag(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val frameworkLocaleManager = context.getSystemService(FrameworkLocaleManager::class.java)
            return frameworkLocaleManager?.applicationLocales?.toLanguageTags().orEmpty()
        }
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}
