package com.example.hatchtracker.di

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.BirdEntityDao
import com.example.hatchtracker.data.FlockEntityDao
import com.example.hatchtracker.data.IncubationEntityDao
import com.example.hatchtracker.data.IncubationMeasurementDao
import com.example.hatchtracker.data.EggProductionDao
import com.example.hatchtracker.data.AssetDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.repository.EggProductionRepository
import com.example.hatchtracker.data.repository.UnitCostProviderImpl
import com.example.hatchtracker.domain.pricing.UnitCostProvider
import com.example.hatchtracker.domain.hatchy.routing.HatchyAssistantRepository
import com.example.hatchtracker.domain.hatchy.routing.IHatchyContextProvider
import com.example.hatchtracker.domain.hatchy.routing.ILexiconRegistry
import com.example.hatchtracker.domain.hatchy.routing.LexiconRegistry
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.example.hatchtracker.billing.Entitlements
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.functions.FirebaseFunctions

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBirdRepository(
        database: AppDatabase,
        birdDao: BirdEntityDao,
        syncCoordinator: CoreDataSyncCoordinator,
        auth: FirebaseAuth,
        entitlements: Entitlements,
        domainEventLogger: DomainEventLogger
    ): BirdRepository {
        return BirdRepository(database, birdDao, syncCoordinator, auth, entitlements, domainEventLogger)
    }

    @Provides
    @Singleton
    fun provideFlockRepository(
        database: AppDatabase,
        flockDao: FlockEntityDao,
        syncCoordinator: CoreDataSyncCoordinator,
        auth: FirebaseAuth,
        entitlements: Entitlements,
        domainEventLogger: DomainEventLogger
    ): FlockRepository {
        return FlockRepository(database, flockDao, syncCoordinator, auth, entitlements, domainEventLogger)
    }

    @Provides
    @Singleton
    fun provideIncubationRepository(
        database: AppDatabase,
        incubationDao: IncubationEntityDao,
        birdDao: BirdEntityDao,
        incubationMeasurementDao: IncubationMeasurementDao,
        syncCoordinator: CoreDataSyncCoordinator,
        auth: FirebaseAuth,
        entitlements: Entitlements,
        domainEventLogger: DomainEventLogger,
        breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository,
        breedingAnalyzer: com.example.hatchtracker.core.common.BreedingAnalyzer
    ): IncubationRepository {
        return IncubationRepository(
            database,
            incubationDao,
            birdDao,
            incubationMeasurementDao,
            syncCoordinator,
            auth,
            entitlements,
            domainEventLogger,
            breedStandardRepository,
            breedingAnalyzer
        )
    }

    @Provides
    @Singleton
    fun provideBreedRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore
    ): com.example.hatchtracker.data.repository.BreedRepository {
        return com.example.hatchtracker.data.repository.BreedRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideBreedStandardRepository(): com.example.hatchtracker.data.repository.BreedStandardRepository {
        return com.example.hatchtracker.data.repository.BreedStandardRepository()
    }

    @Provides
    @Singleton
    fun provideHatchyBreedStandardRepository(
        repository: com.example.hatchtracker.data.repository.BreedStandardRepository
    ): com.example.hatchtracker.domain.hatchy.routing.IBreedStandardRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideDeviceRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        auth: com.google.firebase.auth.FirebaseAuth,
        domainEventLogger: DomainEventLogger
    ): com.example.hatchtracker.data.repository.DeviceRepository {
        return com.example.hatchtracker.data.repository.DeviceRepository(firestore, auth, domainEventLogger)
    }

    @Provides
    @Singleton
    fun provideDeviceCatalogRepository(): com.example.hatchtracker.data.repository.DeviceCatalogRepository {
        return com.example.hatchtracker.data.repository.DeviceCatalogRepository()
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }

    @Provides
    @Singleton
    fun provideSupportRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        functions: FirebaseFunctions
    ): com.example.hatchtracker.data.repository.SupportRepository {
        return com.example.hatchtracker.data.repository.SupportRepository(firestore, functions)
    }

    @Provides
    @Singleton
    fun provideBreedingAnalyzer(
        breedRepository: com.example.hatchtracker.data.repository.BreedStandardRepository
    ): com.example.hatchtracker.core.common.BreedingAnalyzer {
        return com.example.hatchtracker.core.common.BreedingAnalyzer(breedRepository)
    }

    @Provides
    @Singleton
    fun provideBreedingRepository(
        breedingDao: com.example.hatchtracker.data.BreedingDao,
        birdDao: com.example.hatchtracker.data.BirdEntityDao
    ): com.example.hatchtracker.data.repository.BreedingRepository {
        return com.example.hatchtracker.data.repository.BreedingRepository(breedingDao, birdDao)
    }


    @Provides
    @Singleton
    fun provideFinancialRepository(
        financialDao: com.example.hatchtracker.data.FinancialDao,
        birdDao: BirdEntityDao,
        incubationDao: IncubationEntityDao,
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        auth: com.google.firebase.auth.FirebaseAuth,
        domainEventLogger: DomainEventLogger
    ): com.example.hatchtracker.data.repository.FinancialRepository {
        return com.example.hatchtracker.data.repository.FinancialRepository(
            financialDao,
            birdDao,
            incubationDao,
            firestore,
            auth,
            domainEventLogger
        )
    }

    @Provides
    @Singleton
    fun provideNurseryRepository(
        database: AppDatabase,
        graduationListener: com.example.hatchtracker.domain.breeding.FlockletGraduationListener,
        flockRepository: FlockRepository,
        entitlements: Entitlements,
        domainEventLogger: DomainEventLogger,
        breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository
    ): com.example.hatchtracker.data.repository.NurseryRepository {
        return com.example.hatchtracker.data.repository.NurseryRepository(
            database,
            graduationListener,
            flockRepository,
            entitlements,
            domainEventLogger,
            breedStandardRepository
        )
    }

    @Provides
    @Singleton
    fun provideTraitPromotionRepository(
        traitPromotionDao: com.example.hatchtracker.data.TraitPromotionDao
    ): com.example.hatchtracker.data.repository.TraitPromotionRepository {
        return com.example.hatchtracker.data.repository.TraitPromotionRepository(traitPromotionDao)
    }

    @Provides
    @Singleton
    fun provideInboxNotificationRepository(
        inboxNotificationDao: com.example.hatchtracker.data.InboxNotificationDao
    ): com.example.hatchtracker.data.repository.InboxNotificationRepository {
        return com.example.hatchtracker.data.repository.InboxNotificationRepository(inboxNotificationDao)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        auth: com.google.firebase.auth.FirebaseAuth
    ): com.example.hatchtracker.data.repository.UserRepository {
        return com.example.hatchtracker.data.repository.UserRepository(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideBillingRepository(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        auth: com.google.firebase.auth.FirebaseAuth,
        userRepository: com.example.hatchtracker.data.repository.UserRepository
    ): com.example.hatchtracker.data.repository.BillingRepository {
        return com.example.hatchtracker.data.repository.BillingRepository(context, auth, userRepository)
    }

    @Provides
    @Singleton
    fun provideSalesBatchRepository(
        database: AppDatabase,
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        auth: com.google.firebase.auth.FirebaseAuth
    ): com.example.hatchtracker.data.repository.SalesBatchRepository {
        return com.example.hatchtracker.data.repository.SalesBatchRepository(database, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideEggProductionRepository(
        db: AppDatabase,
        dao: EggProductionDao,
        syncCoordinator: CoreDataSyncCoordinator,
        financialRepository: com.example.hatchtracker.data.repository.FinancialRepository,
        unitCostProvider: UnitCostProvider,
        auth: FirebaseAuth,
        domainEventLogger: DomainEventLogger
    ): EggProductionRepository {
        return EggProductionRepository(
            db, dao, syncCoordinator, unitCostProvider, domainEventLogger, auth
        )
    }

    @Provides
    @Singleton
    fun provideUnitCostProvider(
        financialRepository: com.example.hatchtracker.data.repository.FinancialRepository,
        eggProductionDao: EggProductionDao,
        incubationRepository: IncubationRepository,
        assetDao: AssetDao
    ): UnitCostProvider {
        return UnitCostProviderImpl(
            financialRepository,
            eggProductionDao,
            incubationRepository,
            assetDao
        )
    }

    @Provides
    @Singleton
    fun provideCatalogRepository(database: AppDatabase): com.example.hatchtracker.data.repository.CatalogRepository {
        return com.example.hatchtracker.data.repository.CatalogRepository(database)
    }

    @Provides
    @Singleton
    fun provideEggSalesService(
        database: AppDatabase,
        domainEventLogger: DomainEventLogger
    ): com.example.hatchtracker.data.service.EggSalesService {
        return com.example.hatchtracker.data.service.EggSalesService(database, domainEventLogger)
    }

    @Provides
    @Singleton
    fun provideRebuildDelegate(
        database: AppDatabase,
        eggProductionRepository: EggProductionRepository
    ): com.example.hatchtracker.domain.hatchy.DomainEventReplayer.RebuildDelegate {
        return com.example.hatchtracker.data.service.EggProductionRebuildDelegate(database, eggProductionRepository)
    }

    @Provides
    @Singleton
    fun provideDomainEventReplayer(
        delegate: com.example.hatchtracker.domain.hatchy.DomainEventReplayer.RebuildDelegate
    ): com.example.hatchtracker.domain.hatchy.DomainEventReplayer {
        return com.example.hatchtracker.domain.hatchy.DomainEventReplayer(delegate)
    }

    @Provides
    @Singleton
    fun provideSimulationRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore
    ): com.example.hatchtracker.data.repository.SimulationRepository {
        return com.example.hatchtracker.data.repository.SimulationRepository()
    }

    @Provides
    @Singleton
    fun provideFunctionsRepository(
        functions: FirebaseFunctions
    ): com.example.hatchtracker.data.repository.FunctionsRepository {
        return com.example.hatchtracker.data.repository.FunctionsRepository(functions)
    }

    @Provides
    @Singleton
    fun provideHatchyAssistantRepository(
        impl: com.example.hatchtracker.data.repository.HatchyAssistantRepositoryImpl
    ): HatchyAssistantRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideHatchyContextProvider(
        provider: com.example.hatchtracker.core.navigation.HatchyContextProvider
    ): IHatchyContextProvider {
        return provider
    }

    @Provides
    @Singleton
    fun provideLexiconRegistry(
        registry: LexiconRegistry
    ): ILexiconRegistry {
        return registry
    }

    @Provides
    @Singleton
    fun provideBreedingSimulationEngine(): com.example.hatchtracker.domain.breeding.BreedingSimulationEngine {
        return com.example.hatchtracker.domain.breeding.BreedingSimulationEngine()
    }

    @Provides
    @Singleton
    fun provideGeneticProfileResolver(
        breedRepository: com.example.hatchtracker.data.repository.BreedRepository
    ): com.example.hatchtracker.domain.breeding.GeneticProfileResolver {
        return com.example.hatchtracker.domain.breeding.GeneticProfileResolver(breedRepository)
    }

    @Provides
    @Singleton
    fun providePopulationProvider(
        birdRepository: BirdRepository,
        breedRepository: com.example.hatchtracker.data.repository.BreedRepository
    ): com.example.hatchtracker.domain.breeding.PopulationProvider {
        return com.example.hatchtracker.data.breeding.PopulationProviderImpl(
            birdRepository,
            breedRepository
        )
    }

    @Provides
    @Singleton
    fun provideDomainBreedingProgramRepository(
        repository: com.example.hatchtracker.data.repository.BreedingProgramRepository
    ): com.example.hatchtracker.domain.breeding.BreedingProgramRepository {
        return object : com.example.hatchtracker.domain.breeding.BreedingProgramRepository {
            override fun observePlans(userId: String) = repository.observePlans(userId)
            override suspend fun getPlan(planId: String) = repository.getPlan(planId)
            override suspend fun updatePlan(plan: com.example.hatchtracker.data.models.BreedingProgram) =
                repository.updatePlan(plan)
        }
    }

    @Provides
    @Singleton
    fun provideDomainBirdRepository(
        repository: BirdRepository
    ): com.example.hatchtracker.domain.breeding.BirdRepository {
        return object : com.example.hatchtracker.domain.breeding.BirdRepository {
            override val allBirds = repository.allBirds
        }
    }

    @Provides
    @Singleton
    fun provideDomainFlockRepository(
        repository: FlockRepository
    ): com.example.hatchtracker.domain.repo.FlockRepository {
        return object : com.example.hatchtracker.domain.repo.FlockRepository {
            override val allActiveFlocks = repository.allActiveFlocks

            override suspend fun getFlockById(id: Long): com.example.hatchtracker.model.Flock? {
                return repository.getFlockById(id)
            }

            override fun getFlockFlow(id: Long) = repository.getFlockFlow(id)

            override suspend fun insertFlock(flock: com.example.hatchtracker.model.Flock): Long {
                return repository.insertFlock(flock)
            }

            override suspend fun updateFlock(flock: com.example.hatchtracker.model.Flock) {
                repository.updateFlock(flock)
            }

            override suspend fun deleteFlock(flock: com.example.hatchtracker.model.Flock) {
                repository.deleteFlock(flock)
            }

            override fun getBirdCountForFlock(flockId: Long) = repository.getBirdCountForFlock(flockId)

            override suspend fun refreshFlockBreeds(flockId: Long) {
                repository.refreshFlockBreeds(flockId)
            }
        }
    }

    @Provides
    @Singleton
    fun provideDomainIncubationRepository(
        repository: IncubationRepository
    ): com.example.hatchtracker.domain.repo.IncubationRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideAuthProvider(
        sessionManager: com.example.hatchtracker.auth.SessionManager
    ): com.example.hatchtracker.domain.breeding.AuthProvider {
        return object : com.example.hatchtracker.domain.breeding.AuthProvider {
            override val currentUserId: String?
                get() = sessionManager.getCurrentUser()?.uid
        }
    }

}
