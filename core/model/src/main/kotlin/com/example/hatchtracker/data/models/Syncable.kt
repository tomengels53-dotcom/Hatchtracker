package com.example.hatchtracker.data.models

/**
 * Interface for entities that can be synchronized between local and remote data sources.
 */
interface Syncable {
    val syncId: String
    val syncState: SyncState
    val syncError: String?
    val lastModified: Long
    val deleted: Boolean
    val cloudUpdatedAt: Long?
}
