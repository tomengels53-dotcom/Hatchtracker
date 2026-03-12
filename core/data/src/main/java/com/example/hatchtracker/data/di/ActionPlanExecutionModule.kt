package com.example.hatchtracker.data.di

import com.example.hatchtracker.data.repository.FlockletReadRepositoryImpl
import com.example.hatchtracker.domain.breeding.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ActionPlanExecutionModule {

    @Binds
    @Singleton
    abstract fun bindFlockletReadRepository(
        impl: FlockletReadRepositoryImpl
    ): FlockletReadRepository

    @Binds
    @Singleton
    abstract fun bindFlockletGraduationListener(
        service: FlockGraduationService
    ): FlockletGraduationListener
}
