package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.Equipment
import kotlinx.coroutines.flow.Flow

interface EquipmentRepository {
    fun getUserEquipment(): Flow<List<Equipment>>
}
