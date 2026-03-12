package com.example.hatchtracker.di

import com.example.hatchtracker.domain.breeding.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BreedingModule {

    @Provides
    @Singleton
    fun provideBreedingStrategyService(
        populationProvider: PopulationProvider,
        searchEngine: StrategySearchEngine,
        roadmapArchitect: StagedCrossArchitect
    ): BreedingStrategyService {
        return BreedingStrategyService(populationProvider, searchEngine, roadmapArchitect)
    }

    @Provides
    @Singleton
    fun provideStrategySearchEngine(
        breedingPredictionService: BreedingPredictionService,
        planScorer: PlanScorer,
        goalEvaluationEngine: GoalEvaluationEngine,
        genotypePriorService: GenotypePriorService
    ): StrategySearchEngine {
        return StrategySearchEngine(
            breedingPredictionService,
            planScorer,
            goalEvaluationEngine,
            genotypePriorService
        )
    }

    @Provides
    @Singleton
    fun providePlanScorer(): PlanScorer {
        return PlanScorer()
    }

    @Provides
    @Singleton
    fun provideGoalEvaluationEngine(
        breedingGoalEvaluator: BreedingGoalEvaluator
    ): GoalEvaluationEngine {
        return GoalEvaluationEngine(breedingGoalEvaluator)
    }

    @Provides
    @Singleton
    fun provideBreedingGoalEvaluator(): BreedingGoalEvaluator {
        return BreedingGoalEvaluator()
    }

    @Provides
    @Singleton
    fun provideGenotypePriorService(): GenotypePriorService {
        return GenotypePriorService()
    }

    @Provides
    @Singleton
    fun provideGoalTemplateCatalog(): GoalTemplateCatalog {
        return GoalTemplateCatalog()
    }
}
