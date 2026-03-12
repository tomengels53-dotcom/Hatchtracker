package com.example.hatchtracker.data.di

import com.example.hatchtracker.data.repository.InsightRepository
import com.example.hatchtracker.data.repository.CommunityRepositoryImpl
import com.example.hatchtracker.data.repository.MarketplaceRepositoryImpl
import com.example.hatchtracker.data.repository.ModerationActionLogRepository
import com.example.hatchtracker.data.repository.ModerationQueueRepository
import com.example.hatchtracker.data.repository.ReportRepository
import com.example.hatchtracker.data.repository.UserBlockingRepositoryImpl
import com.example.hatchtracker.data.repository.UserSafetyRepository
import com.example.hatchtracker.domain.repository.CommunityPostRepository
import com.example.hatchtracker.domain.repository.MarketplaceComplianceEvaluator
import com.example.hatchtracker.domain.repository.MarketplaceListingRepository
import com.example.hatchtracker.domain.repository.UserBlockingRepository
import com.example.hatchtracker.domain.service.BlockingService
import com.example.hatchtracker.domain.service.EntityPassportSnapshotService
import com.example.hatchtracker.domain.service.ExpertiseSignalService
import com.example.hatchtracker.domain.service.InsightGeneratorService
import com.example.hatchtracker.domain.service.ModerationService
import com.example.hatchtracker.domain.service.QuestionRoutingService
import com.example.hatchtracker.domain.service.ShareCardService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunityServiceModule {

    @Provides
    @Singleton
    fun provideExpertiseSignalService(): ExpertiseSignalService = ExpertiseSignalService()

    @Provides
    @Singleton
    fun provideQuestionRoutingService(
        expertiseSignalService: ExpertiseSignalService
    ): QuestionRoutingService = QuestionRoutingService(expertiseSignalService)

    @Provides
    @Singleton
    fun provideShareCardService(): ShareCardService = ShareCardService()

    @Provides
    @Singleton
    fun provideReportRepository(firestore: FirebaseFirestore): ReportRepository = ReportRepository(firestore)

    @Provides
    @Singleton
    fun provideInsightRepository(firestore: FirebaseFirestore): InsightRepository = InsightRepository(firestore)

    @Provides
    @Singleton
    fun provideInsightGeneratorService(): InsightGeneratorService = InsightGeneratorService()

    @Provides
    @Singleton
    fun provideModerationQueueRepository(firestore: FirebaseFirestore): ModerationQueueRepository = ModerationQueueRepository(firestore)

    @Provides
    @Singleton
    fun provideCommunityRepositoryImpl(firestore: FirebaseFirestore): CommunityRepositoryImpl =
        CommunityRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideCommunityPostRepository(
        impl: CommunityRepositoryImpl
    ): CommunityPostRepository = impl

    @Provides
    @Singleton
    fun provideMarketplaceRepositoryImpl(firestore: FirebaseFirestore): MarketplaceRepositoryImpl =
        MarketplaceRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideMarketplaceListingRepository(
        impl: MarketplaceRepositoryImpl
    ): MarketplaceListingRepository = impl

    @Provides
    @Singleton
    fun provideMarketplaceComplianceEvaluator(
        impl: MarketplaceRepositoryImpl
    ): MarketplaceComplianceEvaluator = impl

    @Provides
    @Singleton
    fun provideEntityPassportSnapshotService(): EntityPassportSnapshotService =
        EntityPassportSnapshotService()

    @Provides
    @Singleton
    fun provideModerationActionLogRepository(firestore: FirebaseFirestore): ModerationActionLogRepository = ModerationActionLogRepository(firestore)

    @Provides
    @Singleton
    fun provideUserSafetyRepository(firestore: FirebaseFirestore): UserSafetyRepository = UserSafetyRepository(firestore)

    @Provides
    @Singleton
    fun provideModerationService(
        reportRepository: ReportRepository,
        safetyRepository: UserSafetyRepository,
        logRepository: ModerationActionLogRepository
    ): ModerationService = ModerationService(reportRepository, safetyRepository, logRepository)

    @Provides
    @Singleton
    fun provideUserBlockingRepository(firestore: FirebaseFirestore): UserBlockingRepository =
        UserBlockingRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideBlockingService(userBlockingRepository: UserBlockingRepository): BlockingService =
        BlockingService(userBlockingRepository)
}
