package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.TraitPromotionDao
import com.example.hatchtracker.data.models.TraitPromotion
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraitPromotionRepository @Inject constructor(
    private val traitPromotionDao: TraitPromotionDao
) {

    fun getAllPromotionsFlow(): Flow<List<TraitPromotion>> {
        return traitPromotionDao.getAllPromotionsFlow()
    }

    suspend fun getPromotionsByBird(birdId: Long): List<TraitPromotion> {
        return traitPromotionDao.getPromotionsByBird(birdId)
    }
}
