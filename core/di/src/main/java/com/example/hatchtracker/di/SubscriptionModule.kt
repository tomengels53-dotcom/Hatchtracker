package com.example.hatchtracker.di

import com.example.hatchtracker.billing.DefaultEntitlements
import com.example.hatchtracker.billing.Entitlements
import com.example.hatchtracker.billing.SubscriptionStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for subscription and entitlements dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object SubscriptionModule {
    
    @Provides
    @Singleton
    fun provideEntitlements(
        subscriptionStateManager: SubscriptionStateManager
    ): Entitlements {
        return DefaultEntitlements(subscriptionStateManager)
    }
}
