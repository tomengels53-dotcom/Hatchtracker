package com.example.hatchtracker.di

import android.content.Context
import android.os.StrictMode
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.BirdEntityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideBirdEntityDao(database: AppDatabase): BirdEntityDao {
        return database.birdDao()
    }

    @Provides
    fun provideFlockEntityDao(database: AppDatabase): com.example.hatchtracker.data.FlockEntityDao {
        return database.flockDao()
    }

    @Provides
    fun provideIncubationEntityDao(database: AppDatabase): com.example.hatchtracker.data.IncubationEntityDao {
        return database.incubationDao()
    }

    @Provides
    fun provideFlockletDao(database: AppDatabase): com.example.hatchtracker.data.FlockletDao {
        return database.flockletDao()
    }

    @Provides
    fun provideFinancialDao(database: AppDatabase): com.example.hatchtracker.data.FinancialDao {
        return database.financialDao()
    }

    @Provides
    fun provideBreedingDao(database: AppDatabase): com.example.hatchtracker.data.BreedingDao {
        return database.breedingDao()
    }

    @Provides
    fun provideSalesBatchDao(database: AppDatabase): com.example.hatchtracker.data.SalesBatchDao {
        return database.salesBatchDao()
    }

    @Provides
    fun provideSpeciesDao(database: AppDatabase): com.example.hatchtracker.data.SpeciesDao {
        return database.speciesDao()
    }

    @Provides
    fun provideTraitPromotionDao(database: AppDatabase): com.example.hatchtracker.data.TraitPromotionDao {
        return database.traitPromotionDao()
    }

    @Provides
    fun provideNotificationHistoryDao(database: AppDatabase): com.example.hatchtracker.data.NotificationHistoryDao {
        return database.notificationHistoryDao()
    }

    @Provides
    fun provideInboxNotificationDao(database: AppDatabase): com.example.hatchtracker.data.InboxNotificationDao {
        return database.inboxNotificationDao()
    }

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): com.example.hatchtracker.data.SyncQueueDao {
        return database.syncQueueDao()
    }

    @Provides
    fun provideBreedingProgramDao(database: AppDatabase): com.example.hatchtracker.data.BreedingProgramDao {
        return database.breedingProgramDao()
    }

    @Provides
    fun provideEggProductionDao(database: AppDatabase): com.example.hatchtracker.data.EggProductionDao {
        return database.eggProductionDao()
    }

    @Provides
    fun provideIncubationMeasurementDao(database: AppDatabase): com.example.hatchtracker.data.IncubationMeasurementDao {
        return database.incubationMeasurementDao()
    }

    @Provides
    fun provideAssetDao(database: AppDatabase): com.example.hatchtracker.data.AssetDao {
        return database.assetDao()
    }

    @Provides
    fun provideAssetAllocationDao(database: AppDatabase): com.example.hatchtracker.data.AssetAllocationDao {
        return database.assetAllocationDao()
    }

    @Provides
    fun provideCostBasisLedgerDao(database: AppDatabase): com.example.hatchtracker.data.CostBasisLedgerDao {
        return database.costBasisLedgerDao()
    }

    @Provides
    fun provideDomainEventDao(database: AppDatabase): com.example.hatchtracker.data.DomainEventDao {
        return database.domainEventDao()
    }

    @Provides
    @Singleton
    fun provideFirestore(): com.google.firebase.firestore.FirebaseFirestore {
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth {
        // FirebaseAuth initialization may trigger StrictMode custom slow calls via keystore APIs.
        val policy = StrictMode.getThreadPolicy()
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        return try {
            com.google.firebase.auth.FirebaseAuth.getInstance()
        } finally {
            StrictMode.setThreadPolicy(policy)
        }
    }
}
