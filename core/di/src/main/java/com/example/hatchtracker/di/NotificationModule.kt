package com.example.hatchtracker.di

import android.content.Context
import com.example.hatchtracker.data.InboxNotificationDao
import com.example.hatchtracker.data.NotificationHistoryDao
import com.example.hatchtracker.notifications.NotificationEngine
import com.example.hatchtracker.notifications.NotificationPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationPreferences(@ApplicationContext context: Context): NotificationPreferences {
        return NotificationPreferences(context)
    }

    @Provides
    @Singleton
    fun provideNotificationEngine(
        @ApplicationContext context: Context,
        historyDao: NotificationHistoryDao,
        inboxDao: InboxNotificationDao,
        prefs: NotificationPreferences
    ): NotificationEngine {
        return NotificationEngine(historyDao, inboxDao, prefs, context)
    }
}
