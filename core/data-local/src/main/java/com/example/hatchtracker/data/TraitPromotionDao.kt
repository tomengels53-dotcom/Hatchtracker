package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.TraitPromotion
import kotlinx.coroutines.flow.Flow

@Dao
interface TraitPromotionDao {
    @Insert
    suspend fun insertPromotion(promotion: TraitPromotion): Long

    @Query("SELECT * FROM trait_promotions ORDER BY timestamp DESC")
    fun getAllPromotionsFlow(): Flow<List<TraitPromotion>>

    @Query("SELECT * FROM trait_promotions WHERE birdId = :birdId")
    suspend fun getPromotionsByBird(birdId: Long): List<TraitPromotion>
}
