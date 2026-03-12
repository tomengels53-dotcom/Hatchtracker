package com.example.hatchtracker.model

/**
 * Represents the current synchronization state of a local record.
 */
enum class SyncState {
    /** Record is synced with the cloud. */
    SYNCED,
    
    /** Record has local changes that need to be pushed to the cloud. */
    PENDING,
    
    /** Sync failed. Check the error log for details. */
    FAILED
}
