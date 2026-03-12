package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import kotlinx.coroutines.flow.StateFlow

interface IHatchyContextProvider {
    val context: StateFlow<HatchyContext>
}
