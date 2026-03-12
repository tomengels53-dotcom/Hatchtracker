@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.notifications

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hatch_notification_prefs", Context.MODE_PRIVATE)

    var isNotificationsEnabled: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_ENABLED, true) }
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    // 24-hour format, e.g., 22 for 10 PM
    var isPushNotificationsEnabled: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true) }
        set(value) = prefs.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, value).apply()

    var quietHoursStart: Int
        get() = withAllowedDiskReads { prefs.getInt(KEY_QUIET_HOURS_START, 22) }
        set(value) = prefs.edit().putInt(KEY_QUIET_HOURS_START, value).apply()

    // 24-hour format, e.g., 7 for 7 AM
    var quietHoursEnd: Int
        get() = withAllowedDiskReads { prefs.getInt(KEY_QUIET_END, 7) }
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value).apply()

    var isProUser: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_PRO_USER, false) }
        set(value) = prefs.edit().putBoolean(KEY_PRO_USER, value).apply()

    var isIncubationRemindersEnabled: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_INCUBATION_REMIDERS, true) }
        set(value) = prefs.edit().putBoolean(KEY_INCUBATION_REMIDERS, value).apply()

    var isNurseryRemindersEnabled: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_NURSERY_REMIDERS, false) }
        set(value) = prefs.edit().putBoolean(KEY_NURSERY_REMIDERS, value).apply()

    var isFlockRemindersEnabled: Boolean
        get() = withAllowedDiskReads { prefs.getBoolean(KEY_FLOCK_REMINDERS, false) }
        set(value) = prefs.edit().putBoolean(KEY_FLOCK_REMINDERS, value).apply()

    fun canSendReminderToday(category: ReminderCategory): Boolean {
        val lastDate = withAllowedDiskReads {
            prefs.getString("${KEY_LAST_REMINDER_PREFIX}_${category.name}", "")
        }
        val today = java.time.LocalDate.now().toString()
        return lastDate != today
    }

    fun markReminderSent(category: ReminderCategory) {
        val today = java.time.LocalDate.now().toString()
        prefs.edit().putString("${KEY_LAST_REMINDER_PREFIX}_${category.name}", today).apply()
    }

    fun isReminderDismissed(reminderId: String): Boolean {
        return withAllowedDiskReads {
            prefs.getBoolean("${KEY_DISMISSED_PREFIX}_$reminderId", false)
        }
    }

    fun dismissReminder(reminderId: String) {
        prefs.edit().putBoolean("${KEY_DISMISSED_PREFIX}_$reminderId", true).apply()
    }

    companion object {
        private const val KEY_ENABLED = "notifications_enabled"
        private const val KEY_PUSH_NOTIFICATIONS = "push_notifications_enabled"
        private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        private const val KEY_QUIET_END = "quiet_hours_end"
        private const val KEY_PRO_USER = "pro_user_status"
        
        private const val KEY_INCUBATION_REMIDERS = "reminders_incubation_enabled"
        private const val KEY_NURSERY_REMIDERS = "reminders_nursery_enabled"
        private const val KEY_FLOCK_REMINDERS = "flock_reminders_enabled"
        
        private const val KEY_LAST_REMINDER_PREFIX = "last_reminder_date"
        private const val KEY_DISMISSED_PREFIX = "dismissed_reminder"
    }

    fun isInQuietHours(species: String? = null, incubationSpecificStart: Int? = null, incubationSpecificEnd: Int? = null): Boolean {
        if (!isNotificationsEnabled) return true

        // 1. Incubation Specific
        if (incubationSpecificStart != null && incubationSpecificEnd != null) {
            return checkQuietHours(incubationSpecificStart, incubationSpecificEnd)
        }

        // 2. Species Specific
        if (species != null) {
            val speciesHrs = getSpeciesQuietHours(species)
            if (speciesHrs != null) {
                return checkQuietHours(speciesHrs.first, speciesHrs.second)
            }
        }

        // 3. Global Default
        return checkQuietHours(quietHoursStart, quietHoursEnd)
    }

    private fun checkQuietHours(start: Int, end: Int): Boolean {
        val currentHour = java.time.LocalTime.now().hour
        return if (start <= end) {
            currentHour in start until end
        } else {
            currentHour >= start || currentHour < end
        }
    }

    fun setSpeciesQuietHours(species: String, start: Int, end: Int) {
        prefs.edit().putString("quiet_$species", "$start|$end").apply()
    }

    fun getSpeciesQuietHours(species: String): Pair<Int, Int>? {
        val raw = withAllowedDiskReads {
            prefs.getString("quiet_$species", null)
        } ?: return null
        val parts = raw.split("|")
        if (parts.size != 2) return null
        return try {
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: NumberFormatException) {
            null
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
}

