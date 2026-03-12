package com.example.hatchtracker.domain.breeding

data class FlockletRef(
    val cloudId: String
)

interface FlockletGraduationListener {
    suspend fun onFlockletGraduated(
        flockletLocalId: Long,
        targetFlockLocalId: Long
    )
}

interface FlockletReadRepository {
    suspend fun getFlockletById(id: Long): FlockletRef?
}

interface AuthProvider {
    val currentUserId: String?
}
