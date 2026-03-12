@file:android.annotation.SuppressLint("NewApi", "MissingPermission")

package com.example.hatchtracker.notifications.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.hatchtracker.data.models.IncubationLike
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.receivers.HatchAlarmReceiver
import java.time.LocalDate
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleLockdownAlarm(incubation: IncubationLike) {
        val config = IncubationManager.getConfig(incubation)
        val startDate = LocalDate.parse(incubation.startDate) // Assumes ISO format
        val lockdownDate = startDate.plusDays(config.lockdownDay.toLong())
        
        // Trigger at 8:00 AM on lockdown day
        val triggerTime = lockdownDate.atTime(8, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerTime > System.currentTimeMillis()) {
            setExactAlarm(incubation.id, "LOCKDOWN", triggerTime)
        }
    }

    fun scheduleHatchWindowAlarm(incubation: IncubationLike) {
        val config = IncubationManager.getConfig(incubation)
        val startDate = LocalDate.parse(incubation.startDate)
        val hatchWindowDate = startDate.plusDays(config.hatchWindowStartDay.toLong())

        val triggerTime = hatchWindowDate.atTime(8, 0)
             .atZone(ZoneId.systemDefault())
             .toInstant()
             .toEpochMilli()

        if (triggerTime > System.currentTimeMillis()) {
            setExactAlarm(incubation.id, "HATCH_WINDOW", triggerTime)
        }
    }

    private fun setExactAlarm(incubationId: Long, type: String, triggerTime: Long) {
        val intent = Intent(context, HatchAlarmReceiver::class.java).apply {
            action = "com.example.hatchtracker.ALARM_TRIGGER"
            putExtra("INCUBATION_ID", incubationId)
            putExtra("ALARM_TYPE", type)
        }

        // Unique RequestCode based on ID and Type hash
        val requestCode = (incubationId.toString() + type).hashCode()
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact if permission missing (should handle permission request in UI)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
             alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarms(incubationId: Long) {
        val types = listOf("LOCKDOWN", "HATCH_WINDOW")
        types.forEach { type ->
            val intent = Intent(context, HatchAlarmReceiver::class.java)
            val requestCode = (incubationId.toString() + type).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}



