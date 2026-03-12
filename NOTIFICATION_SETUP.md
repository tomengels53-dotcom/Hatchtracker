# Hatch Notification Setup Guide

## Overview
This app uses WorkManager to schedule local notifications that alert users 1 day before an incubation's expected hatch date.

## Files Created

### 1. `NotificationHelper.kt`
**Location:** `app/src/main/java/com/example/hatchtracker/NotificationHelper.kt`

This file contains:
- `NotificationHelper` object: Manages notification channels and scheduling
- `HatchNotificationWorker` class: WorkManager worker that displays notifications

### 2. Updated Files
- `app/build.gradle.kts`: Added WorkManager dependency
- `app/src/main/AndroidManifest.xml`: Added POST_NOTIFICATIONS permission
- `app/src/main/java/com/example/hatchtracker/MainActivity.kt`: Creates notification channel on app start
- `app/src/main/java/com/example/hatchtracker/AddIncubationScreen.kt`: Schedules notification when incubation is saved

## How It Works

### 1. Notification Channel Creation
The notification channel is created in `MainActivity.onCreate()`:
```kotlin
NotificationHelper.createNotificationChannel(this)
```
This is required for Android 8.0+ and must be called before scheduling any notifications.

### 2. Scheduling Notifications
When a new incubation is saved in `AddIncubationScreen`, the notification is automatically scheduled:
```kotlin
NotificationHelper.scheduleHatchNotification(
    context = context,
    incubationId = newId,
    birdName = birdName,
    expectedHatch = expectedHatch.trim()
)
```

### 3. Notification Timing
- Notifications are scheduled for **1 day before** the expected hatch date
- If the notification date has already passed, no notification is scheduled
- WorkManager handles the scheduling and ensures notifications are delivered even if the app is closed

### 4. Notification Content
The notification displays:
- **Title:** "Hatch Alert"
- **Message:** "Eggs of [Bird Name] will hatch tomorrow"
- **Expanded Text:** Includes the expected hatch date

## Permissions

### Android 13+ (API 33+)
The app requires the `POST_NOTIFICATIONS` permission, which is already added to the manifest. Users will be prompted to grant this permission when the app first tries to show a notification.

### Requesting Permission (Optional)
If you want to request permission explicitly, you can add this to your Activity:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATION
        )
    }
}
```

## Testing

### Test Notification Immediately
To test notifications immediately, you can call:
```kotlin
NotificationHelper.showNotification(
    context,
    incubationId = 1,
    birdName = "Test Bird",
    expectedHatch = "2024-12-25"
)
```

### Test Scheduled Notification
1. Create a new incubation with an expected hatch date 1-2 days in the future
2. The notification will be scheduled automatically
3. Wait for the notification to appear (or adjust the delay in `NotificationHelper.kt` for testing)

## Canceling Notifications

To cancel a scheduled notification:
```kotlin
NotificationHelper.cancelHatchNotification(context, incubationId)
```

## Customization

### Change Notification Icon
Replace `android.R.drawable.ic_dialog_info` in `NotificationHelper.showNotification()` with your custom icon:
```kotlin
.setSmallIcon(R.drawable.ic_hatch_notification)
```

### Change Notification Timing
Modify the delay calculation in `scheduleHatchNotification()`:
```kotlin
val notificationDate = hatchDate.minusDays(2) // 2 days before instead of 1
```

### Change Notification Message
Modify the notification text in `showNotification()`:
```kotlin
.setContentText("Your custom message here")
```

## WorkManager Benefits

- **Reliable:** Works even if the app is closed or device is rebooted
- **Battery Efficient:** Uses system-level scheduling
- **Handles Edge Cases:** Automatically handles timezone changes, device restarts, etc.

## Troubleshooting

### Notifications Not Appearing
1. Check that notification permission is granted (Android 13+)
2. Verify the notification channel is created
3. Check that the expected hatch date is in the future
4. Verify WorkManager is initialized correctly

### Notifications Appearing at Wrong Time
- Check device timezone settings
- Verify date format is YYYY-MM-DD
- Check that the delay calculation is correct
