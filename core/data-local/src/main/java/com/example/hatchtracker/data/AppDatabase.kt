package com.example.hatchtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.hatchtracker.data.converters.DataConverters
import com.example.hatchtracker.data.models.*

@Database(
    entities = [
        BirdEntity::class,
        FlockEntity::class,
        FlockletEntity::class,
        BreedingRecord::class,
        BreedingProgramEntity::class,
        EggProductionEntity::class,
        BreedLineEntity::class,
        IncubationEntity::class,
        FinancialEntry::class,
        FinancialSummaryEntity::class,
        SalesBatch::class,
        Species::class,
        Breed::class,
        TraitPromotion::class,
        NotificationHistory::class,
        InboxNotification::class,
        SyncQueueEntity::class,
        AssetEntity::class,
        AssetAllocationEventEntity::class,
        CostBasisLedgerEntryEntity::class,
        IncubationMeasurement::class,
        EggReservationEntity::class,
        DomainEventEntity::class,
        EggSaleEntity::class,
        EggSaleAllocationEntity::class
    ],
    version = 31,
    exportSchema = true
)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun birdDao(): BirdEntityDao
    abstract fun flockDao(): FlockEntityDao
    abstract fun flockletDao(): FlockletDao
    abstract fun breedingDao(): BreedingDao
    abstract fun breedingProgramDao(): BreedingProgramDao
    abstract fun eggProductionDao(): EggProductionDao
    abstract fun incubationDao(): IncubationEntityDao
    abstract fun financialDao(): FinancialDao
    abstract fun salesBatchDao(): SalesBatchDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun traitPromotionDao(): TraitPromotionDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun inboxNotificationDao(): InboxNotificationDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun assetDao(): AssetDao
    abstract fun assetAllocationDao(): AssetAllocationDao
    abstract fun costBasisLedgerDao(): CostBasisLedgerDao
    abstract fun incubationMeasurementDao(): IncubationMeasurementDao
    abstract fun eggReservationDao(): EggReservationDao
    abstract fun domainEventDao(): DomainEventDao
    abstract fun eggSaleDao(): EggSaleDao
    abstract fun eggSaleAllocationDao(): EggSaleAllocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to financial_entries
                addColumnIfMissing(db, "financial_entries", "amountNet", "REAL")
                addColumnIfMissing(db, "financial_entries", "amountVAT", "REAL")
                addColumnIfMissing(db, "financial_entries", "amountGross", "REAL")
                addColumnIfMissing(db, "financial_entries", "currency", "TEXT")
                addColumnIfMissing(db, "financial_entries", "vatEnabled", "INTEGER")
                addColumnIfMissing(db, "financial_entries", "vatRate", "REAL")
                addColumnIfMissing(db, "financial_entries", "isRecurring", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "financial_entries", "recurrenceIntervalDays", "INTEGER")
                addColumnIfMissing(db, "financial_entries", "lastRecurrenceDate", "INTEGER")
                addColumnIfMissing(db, "financial_entries", "depreciationMonths", "INTEGER")

                // Backfill legacy data
                db.execSQL("UPDATE financial_entries SET amountNet = amount, amountVAT = 0.0, amountGross = amount, currency = 'EUR', vatEnabled = 0 WHERE vatEnabled IS NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // GeneticProfile.traitOverrides columns for embedded profiles
                addColumnIfMissing(db, "birds", "genetic_traitOverrides", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "birds", "custom_traitOverrides", "TEXT")
                addColumnIfMissing(db, "flocks", "default_traitOverrides", "TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // New tables will be auto-created by Room if standard creation query is supplied, 
                // but standard practice for Room Migration requires executing the CREATE TABLE statements directly
                // if fallbackToDestructiveMigrationFrom isn't relied upon exclusively.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `assets` (`assetId` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `linkedDeviceId` TEXT, `purchaseDateEpochMs` INTEGER NOT NULL, `purchasePrice` REAL NOT NULL, `residualValue` REAL NOT NULL, `depreciationMethod` TEXT NOT NULL, `usefulLifeMonths` INTEGER, `expectedCycles` INTEGER, `cyclesAllocatedCount` INTEGER NOT NULL, `lastAllocatedAtEpochMs` INTEGER, `retiredDateEpochMs` INTEGER, `retirementValue` REAL, `status` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `syncStateInt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `pendingSync` INTEGER NOT NULL, `localUpdatedAt` INTEGER NOT NULL, `serverUpdatedAt` INTEGER, `cloudId` TEXT NOT NULL, PRIMARY KEY(`assetId`))
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asset_allocation_events` (`allocationId` TEXT NOT NULL, `assetId` TEXT NOT NULL, `scopeType` TEXT NOT NULL, `scopeId` TEXT NOT NULL, `periodKey` TEXT NOT NULL, `amount` REAL NOT NULL, `ownerUserId` TEXT NOT NULL, `syncStateInt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `pendingSync` INTEGER NOT NULL, `localUpdatedAt` INTEGER NOT NULL, `serverUpdatedAt` INTEGER, `cloudId` TEXT NOT NULL, PRIMARY KEY(`allocationId`))
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cost_basis_ledger` (`entryId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `amount` REAL NOT NULL, `ownerUserId` TEXT NOT NULL, `syncStateInt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `pendingSync` INTEGER NOT NULL, `localUpdatedAt` INTEGER NOT NULL, `serverUpdatedAt` INTEGER, `cloudId` TEXT NOT NULL, PRIMARY KEY(`entryId`))
                """)
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `incubation_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `incubationId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `temperatureC` REAL, `humidityPercent` INTEGER, `source` TEXT NOT NULL, `rawText` TEXT)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_incubation_measurements_incubationId` ON `incubation_measurements` (`incubationId`)
                """)
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild assets with the current Room schema.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `assets_new` (
                        `assetId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `linkedDeviceId` TEXT,
                        `purchaseDateEpochMs` INTEGER NOT NULL,
                        `purchasePrice` REAL NOT NULL,
                        `residualValue` REAL NOT NULL,
                        `depreciationMethod` TEXT NOT NULL,
                        `usefulLifeMonths` INTEGER,
                        `expectedCycles` INTEGER,
                        `cyclesAllocatedCount` INTEGER NOT NULL,
                        `lastAllocatedAtEpochMs` INTEGER,
                        `retiredDateEpochMs` INTEGER,
                        `retirementValue` REAL,
                        `status` TEXT NOT NULL,
                        `ownerUserId` TEXT NOT NULL,
                        `syncStateInt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `pendingSync` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL,
                        `syncError` TEXT,
                        `localUpdatedAt` INTEGER NOT NULL,
                        `serverUpdatedAt` INTEGER,
                        `cloudId` TEXT NOT NULL,
                        PRIMARY KEY(`assetId`)
                    )
                """)

                db.execSQL("""
                    INSERT INTO `assets_new` (
                        `assetId`, `name`, `category`, `linkedDeviceId`, `purchaseDateEpochMs`, `purchasePrice`,
                        `residualValue`, `depreciationMethod`, `usefulLifeMonths`, `expectedCycles`,
                        `cyclesAllocatedCount`, `lastAllocatedAtEpochMs`, `retiredDateEpochMs`, `retirementValue`,
                        `status`, `ownerUserId`, `syncStateInt`, `updatedAt`, `syncId`, `deleted`, `pendingSync`,
                        `syncState`, `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    )
                    SELECT
                        `assetId`, `name`, `category`, `linkedDeviceId`, `purchaseDateEpochMs`, `purchasePrice`,
                        `residualValue`, `depreciationMethod`, `usefulLifeMonths`, `expectedCycles`,
                        `cyclesAllocatedCount`, `lastAllocatedAtEpochMs`, `retiredDateEpochMs`, `retirementValue`,
                        `status`, `ownerUserId`, `syncStateInt`, `updatedAt`,
                        `assetId` AS `syncId`,
                        CASE WHEN `syncStateInt` = -1 THEN 1 ELSE 0 END AS `deleted`,
                        `pendingSync`,
                        CASE WHEN `syncStateInt` = 0 THEN 'SYNCED' ELSE 'PENDING' END AS `syncState`,
                        NULL AS `syncError`,
                        `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    FROM `assets`
                """)

                db.execSQL("DROP TABLE `assets`")
                db.execSQL("ALTER TABLE `assets_new` RENAME TO `assets`")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild asset_allocation_events with the current Room schema.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asset_allocation_events_new` (
                        `allocationId` TEXT NOT NULL,
                        `assetId` TEXT NOT NULL,
                        `scopeType` TEXT NOT NULL,
                        `scopeId` TEXT NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `ownerUserId` TEXT NOT NULL,
                        `syncStateInt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `pendingSync` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL,
                        `syncError` TEXT,
                        `localUpdatedAt` INTEGER NOT NULL,
                        `serverUpdatedAt` INTEGER,
                        `cloudId` TEXT NOT NULL,
                        PRIMARY KEY(`allocationId`)
                    )
                """)

                db.execSQL("""
                    INSERT INTO `asset_allocation_events_new` (
                        `allocationId`, `assetId`, `scopeType`, `scopeId`, `periodKey`, `amount`,
                        `ownerUserId`, `syncStateInt`, `createdAt`, `syncId`, `deleted`, `pendingSync`,
                        `syncState`, `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    )
                    SELECT
                        `allocationId`, `assetId`, `scopeType`, `scopeId`, `periodKey`, `amount`,
                        `ownerUserId`, `syncStateInt`, `createdAt`,
                        `allocationId` AS `syncId`,
                        CASE WHEN `syncStateInt` = -1 THEN 1 ELSE 0 END AS `deleted`,
                        `pendingSync`,
                        CASE WHEN `syncStateInt` = 0 THEN 'SYNCED' ELSE 'PENDING' END AS `syncState`,
                        NULL AS `syncError`,
                        `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    FROM `asset_allocation_events`
                """)

                db.execSQL("DROP TABLE `asset_allocation_events`")
                db.execSQL("ALTER TABLE `asset_allocation_events_new` RENAME TO `asset_allocation_events`")

                // Rebuild cost_basis_ledger with the current Room schema.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cost_basis_ledger_new` (
                        `entryId` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `ownerUserId` TEXT NOT NULL,
                        `syncStateInt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `pendingSync` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL,
                        `syncError` TEXT,
                        `localUpdatedAt` INTEGER NOT NULL,
                        `serverUpdatedAt` INTEGER,
                        `cloudId` TEXT NOT NULL,
                        PRIMARY KEY(`entryId`)
                    )
                """)

                db.execSQL("""
                    INSERT INTO `cost_basis_ledger_new` (
                        `entryId`, `entityType`, `entityId`, `sourceType`, `amount`, `ownerUserId`,
                        `syncStateInt`, `createdAt`, `syncId`, `deleted`, `pendingSync`, `syncState`,
                        `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    )
                    SELECT
                        `entryId`, `entityType`, `entityId`, `sourceType`, `amount`, `ownerUserId`,
                        `syncStateInt`, `createdAt`,
                        `entryId` AS `syncId`,
                        CASE WHEN `syncStateInt` = -1 THEN 1 ELSE 0 END AS `deleted`,
                        `pendingSync`,
                        CASE WHEN `syncStateInt` = 0 THEN 'SYNCED' ELSE 'PENDING' END AS `syncState`,
                        NULL AS `syncError`,
                        `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                    FROM `cost_basis_ledger`
                """)

                db.execSQL("DROP TABLE `cost_basis_ledger`")
                db.execSQL("ALTER TABLE `cost_basis_ledger_new` RENAME TO `cost_basis_ledger`")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildAssetAllocationEventsTable(db)
                rebuildCostBasisLedgerTable(db)
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildAssetsTable(db)
                rebuildAssetAllocationEventsTable(db)
                rebuildCostBasisLedgerTable(db)
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildAssetsTable(db)
                rebuildAssetAllocationEventsTable(db)
                rebuildCostBasisLedgerTable(db)
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildBreedingActionPlansTable(db)

                // Ensure tables exist even on schema-drifted installs, then add the new column if needed.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `inbox_notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `incubationId` INTEGER NOT NULL,
                        `eventId` TEXT,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `severity` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `snoozedUntil` INTEGER NOT NULL
                    )
                """)
                addColumnIfMissing(db, "inbox_notifications", "eventId", "TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_notifications_eventId` ON `inbox_notifications` (`eventId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notification_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `incubationId` INTEGER NOT NULL,
                        `ruleId` TEXT NOT NULL,
                        `eventId` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
                addColumnIfMissing(db, "notification_history", "eventId", "TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_history_eventId` ON `notification_history` (`eventId`)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Heal schema-drifted installs where version was bumped but breeding_action_plans
                // still has the pre-v20 column set.
                rebuildBreedingActionPlansTable(db)

                // Birds Table
                addColumnIfMissing(db, "birds", "lifecycleStage", "TEXT NOT NULL DEFAULT 'ADULT'")
                addColumnIfMissing(db, "birds", "genetic_traitValues", "TEXT NOT NULL DEFAULT '{}'")
                addColumnIfMissing(db, "birds", "genetic_traitWeights", "TEXT NOT NULL DEFAULT '{}'")
                addColumnIfMissing(db, "birds", "genetic_notes", "TEXT")
                addColumnIfMissing(db, "birds", "genetic_genotypeCalls", "TEXT")
                addColumnIfMissing(db, "birds", "genetic_genotypeVersion", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "birds", "custom_knownGenes", "TEXT")
                addColumnIfMissing(db, "birds", "custom_fixedTraits", "TEXT")
                addColumnIfMissing(db, "birds", "custom_inferredTraits", "TEXT")
                addColumnIfMissing(db, "birds", "custom_unknownTraits", "TEXT")
                addColumnIfMissing(db, "birds", "custom_confidenceLevel", "TEXT")
                addColumnIfMissing(db, "birds", "custom_traitValues", "TEXT DEFAULT '{}'")
                addColumnIfMissing(db, "birds", "custom_traitWeights", "TEXT DEFAULT '{}'")
                addColumnIfMissing(db, "birds", "custom_traitOverrides", "TEXT DEFAULT '[]'")
                addColumnIfMissing(db, "birds", "custom_notes", "TEXT")
                addColumnIfMissing(db, "birds", "custom_genotypeCalls", "TEXT")
                addColumnIfMissing(db, "birds", "custom_genotypeVersion", "INTEGER DEFAULT 1")
                addColumnIfMissing(db, "birds", "ringNumber", "TEXT")
                addColumnIfMissing(db, "birds", "ownerUserId", "TEXT")
                addColumnIfMissing(db, "birds", "cloudId", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "birds", "flockCloudId", "TEXT")
                addColumnIfMissing(db, "birds", "motherCloudId", "TEXT")
                addColumnIfMissing(db, "birds", "fatherCloudId", "TEXT")
                addColumnIfMissing(db, "birds", "serverUpdatedAt", "INTEGER")
                addColumnIfMissing(db, "birds", "localUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "birds", "deleted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "birds", "pendingSync", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "birds", "syncState", "TEXT NOT NULL DEFAULT 'PENDING'")
                addColumnIfMissing(db, "birds", "syncError", "TEXT")

                // Incubations Table
                addColumnIfMissing(db, "incubations", "breeds", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "incubations", "lifecycleStage", "TEXT NOT NULL DEFAULT 'INCUBATING'")
                addColumnIfMissing(db, "incubations", "notes", "TEXT")
                addColumnIfMissing(db, "incubations", "actionPlanId", "TEXT")
                addColumnIfMissing(db, "incubations", "generationIndex", "INTEGER")
                addColumnIfMissing(db, "incubations", "breedLabelOverride", "TEXT")
                addColumnIfMissing(db, "incubations", "breederPoolBirdIds", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "incubations", "ownerUserId", "TEXT")
                addColumnIfMissing(db, "incubations", "cloudId", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "incubations", "flockCloudId", "TEXT")
                addColumnIfMissing(db, "incubations", "birdCloudId", "TEXT")
                addColumnIfMissing(db, "incubations", "fatherBirdCloudId", "TEXT")
                addColumnIfMissing(db, "incubations", "serverUpdatedAt", "INTEGER")
                addColumnIfMissing(db, "incubations", "localUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "incubations", "deleted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "incubations", "pendingSync", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "incubations", "syncState", "TEXT NOT NULL DEFAULT 'PENDING'")
                addColumnIfMissing(db, "incubations", "syncError", "TEXT")

                // Flocks Table
                addColumnIfMissing(db, "flocks", "breeds", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "flocks", "eggCount", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "flocks", "default_knownGenes", "TEXT")
                addColumnIfMissing(db, "flocks", "default_fixedTraits", "TEXT")
                addColumnIfMissing(db, "flocks", "default_inferredTraits", "TEXT")
                addColumnIfMissing(db, "flocks", "default_unknownTraits", "TEXT")
                addColumnIfMissing(db, "flocks", "default_confidenceLevel", "TEXT")
                addColumnIfMissing(db, "flocks", "default_traitValues", "TEXT")
                addColumnIfMissing(db, "flocks", "default_traitWeights", "TEXT")
                addColumnIfMissing(db, "flocks", "default_traitOverrides", "TEXT")
                addColumnIfMissing(db, "flocks", "default_notes", "TEXT")
                addColumnIfMissing(db, "flocks", "default_genotypeCalls", "TEXT")
                addColumnIfMissing(db, "flocks", "default_genotypeVersion", "INTEGER")
                addColumnIfMissing(db, "flocks", "ownerUserId", "TEXT")
                addColumnIfMissing(db, "flocks", "cloudId", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "flocks", "serverUpdatedAt", "INTEGER")
                addColumnIfMissing(db, "flocks", "localUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "flocks", "deleted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "flocks", "pendingSync", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "flocks", "syncState", "TEXT NOT NULL DEFAULT 'PENDING'")
                addColumnIfMissing(db, "flocks", "syncError", "TEXT")

                // Flocklets Table
                addColumnIfMissing(db, "flocklets", "breeds", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "flocklets", "lifecycleStage", "TEXT NOT NULL DEFAULT 'FLOCKLET'")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Repair breeding_action_plans for installs that reached v21 with stale schema.
                rebuildBreedingActionPlansTable(db)
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `egg_reservation` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `incubationId` INTEGER NOT NULL,
                        `productionLogId` TEXT NOT NULL,
                        `reservedCount` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`incubationId`) REFERENCES `incubations`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`productionLogId`) REFERENCES `egg_production`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_reservation_incubationId` ON `egg_reservation` (`incubationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_reservation_productionLogId` ON `egg_reservation` (`productionLogId`)")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the initial domain_event table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `domain_event` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` TEXT NOT NULL DEFAULT '',
                        `entityType` TEXT NOT NULL,
                        `entityId` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `payloadJson` TEXT,
                        `createdAtEpochMillis` INTEGER NOT NULL DEFAULT 0,
                        `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                        `dedupeKey` TEXT
                    )
                """)
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Update domain_event table
                addColumnIfMissing(db, "domain_event", "eventId", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "domain_event", "schemaVersion", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "domain_event", "dedupeKey", "TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_domain_event_eventId` ON `domain_event` (`eventId`)")
                
                db.execSQL("""
                    UPDATE domain_event 
                    SET eventId = (
                        lower(hex(randomblob(4))) || '-' || 
                        lower(hex(randomblob(2))) || '-4' || 
                        substr(lower(hex(randomblob(2))),2) || '-' || 
                        substr('89ab',abs(random()) % 4 + 1, 1) || 
                        substr(lower(hex(randomblob(2))),2) || '-' || 
                        lower(hex(randomblob(6)))
                    )
                    WHERE eventId = '' OR eventId IS NULL
                """)

                // 2. Rebuild sync_queue table
                db.execSQL("DROP TABLE IF EXISTS sync_queue")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `lastAttemptAt` INTEGER,
                        `nextRetryAt` INTEGER,
                        `errorMessage` TEXT
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_queue_eventId` ON `sync_queue` (`eventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_nextRetryAt` ON `sync_queue` (`nextRetryAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_status` ON `sync_queue` (`status`)")

        // 3. Hybrid Genetics Columns (Phase 3)
                addColumnIfMissing(db, "birds", "geneticProfileJson", "TEXT")
                addColumnIfMissing(db, "birds", "customGeneticProfileJson", "TEXT")
                addColumnIfMissing(db, "birds", "geneticsSummary_speciesCode", "TEXT")
                addColumnIfMissing(db, "birds", "geneticsSummary_sexLinkedFlags", "INTEGER")
                addColumnIfMissing(db, "birds", "geneticsSummary_hasLethalCarrier", "INTEGER")
                
                addColumnIfMissing(db, "flocks", "defaultGeneticProfileJson", "TEXT")

                // 4. Organizational Scoping (Phase 5)
                val scopingTables = listOf("birds", "flocks", "incubations", "egg_production", "financial_entries", "flocklets")
                scopingTables.forEach { table ->
                    addColumnIfMissing(db, table, "scopeType", "TEXT NOT NULL DEFAULT 'USER'")
                    addColumnIfMissing(db, table, "scopeId", "TEXT")
                }
            }
        }

        /**
         * Migration 25 → 26:
         * 1. Add soldEggs column to egg_production
         * 2. Rebuild domain_event table (entityType/entityId → aggregateType/aggregateId: String)
         * 3. Create egg_sale table
         * 4. Create egg_sale_allocation table
         */
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: soldEggs on egg_production
                addColumnIfMissing(db, "egg_production", "soldEggs", "INTEGER NOT NULL DEFAULT 0")

                // Step 2: Rebuild domain_event with String aggregateId
                // Old columns: id, eventId, entityType, entityId (Long), eventType, payloadJson,
                //              createdAtEpochMillis, schemaVersion, dedupeKey
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `domain_event_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `aggregateType` TEXT NOT NULL,
                        `aggregateId` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `payloadJson` TEXT,
                        `createdAtEpochMillis` INTEGER NOT NULL DEFAULT 0,
                        `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                        `dedupeKey` TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO `domain_event_new`
                        (`id`, `eventId`, `aggregateType`, `aggregateId`, `eventType`, `payloadJson`,
                         `createdAtEpochMillis`, `schemaVersion`, `dedupeKey`)
                    SELECT `id`, `eventId`, `entityType`, CAST(`entityId` AS TEXT), `eventType`, `payloadJson`,
                           `createdAtEpochMillis`, `schemaVersion`, `dedupeKey`
                    FROM `domain_event`
                """)
                db.execSQL("DROP TABLE `domain_event`")
                db.execSQL("ALTER TABLE `domain_event_new` RENAME TO `domain_event`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_domain_event_eventId` ON `domain_event` (`eventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_domain_event_aggregateType` ON `domain_event` (`aggregateType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_domain_event_aggregateId` ON `domain_event` (`aggregateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_domain_event_eventType` ON `domain_event` (`eventType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_domain_event_createdAtEpochMillis` ON `domain_event` (`createdAtEpochMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_domain_event_createdAt_desc` ON `domain_event` (`createdAtEpochMillis` DESC)")

                // Step 3: egg_sale table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `egg_sale` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `flockId` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `saleDateEpochDay` INTEGER NOT NULL,
                        `pricePerEggCents` INTEGER NOT NULL,
                        `totalRevenueCents` INTEGER NOT NULL,
                        `derivedCostPerEggCents` INTEGER NOT NULL,
                        `totalCogsCents` INTEGER NOT NULL,
                        `notes` TEXT,
                        `cancelled` INTEGER NOT NULL DEFAULT 0,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `syncTime` INTEGER,
                        `scopeType` TEXT NOT NULL DEFAULT 'USER',
                        `scopeId` TEXT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_sale_flockId_saleDateEpochDay` ON `egg_sale` (`flockId`, `saleDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_sale_id_cancelled` ON `egg_sale` (`id`, `cancelled`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_egg_sale_syncId` ON `egg_sale` (`syncId`)")

                // Step 4: egg_sale_allocation table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `egg_sale_allocation` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `productionLogId` TEXT NOT NULL,
                        `allocatedCount` INTEGER NOT NULL,
                        FOREIGN KEY (`saleId`) REFERENCES `egg_sale` (`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_sale_allocation_saleId` ON `egg_sale_allocation` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_sale_allocation_productionLogId` ON `egg_sale_allocation` (`productionLogId`)")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Incubation additions
                db.execSQL("ALTER TABLE incubations ADD COLUMN costBasisCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE incubations ADD COLUMN assetAllocationCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE incubations ADD COLUMN costBasisSchemaVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE incubations ADD COLUMN isCostFrozen INTEGER NOT NULL DEFAULT 0")

                // Flocklet additions
                db.execSQL("ALTER TABLE flocklets ADD COLUMN costBasisCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flocklets ADD COLUMN costBasisSourceRef TEXT")
                db.execSQL("ALTER TABLE flocklets ADD COLUMN costBasisSchemaVersion INTEGER NOT NULL DEFAULT 1")

                // Bird additions
                db.execSQL("ALTER TABLE birds ADD COLUMN costBasisCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE birds ADD COLUMN costBasisSourceRef TEXT")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add quantitativeTraits JSON representation for embedded GeneticProfile
                addColumnIfMissing(db, "birds", "genetic_quantitativeTraits", "TEXT NOT NULL DEFAULT '{}'")
                addColumnIfMissing(db, "birds", "custom_quantitativeTraits", "TEXT")
                addColumnIfMissing(db, "flocks", "default_quantitativeTraits", "TEXT")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add missing indices on foreign keys to optimize queries
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_birds_flockId` ON `birds` (`flockId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_birds_motherId` ON `birds` (`motherId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_birds_fatherId` ON `birds` (`fatherId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_birds_incubationId` ON `birds` (`incubationId`)")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_incubations_birdId` ON `incubations` (`birdId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_incubations_fatherBirdId` ON `incubations` (`fatherBirdId`)")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flocklets_hatchId` ON `flocklets` (`hatchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flocklets_movedToFlockId` ON `flocklets` (`movedToFlockId`)")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Additive columns for Birds
                addColumnIfMissing(db, "birds", "generationLabel", "TEXT")
                addColumnIfMissing(db, "birds", "breedComposition", "TEXT NOT NULL DEFAULT '[]'")
                
                // Additive columns for Breeds (Catalog expansion)
                addColumnIfMissing(db, "breeds", "breedType", "TEXT NOT NULL DEFAULT 'HERITAGE'")
                addColumnIfMissing(db, "breeds", "geneticTags", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "breeds", "crossbreedingNotes", "TEXT")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "birds", "hatchDate", "TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hatch_tracker_db"
                )
                .addMigrations(
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30,
                    MIGRATION_30_31
                )
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, type: String) {
            val cursor = db.query("PRAGMA table_info($table)")
            var exists = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == column) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            }
        }

        private fun tableHasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
            val cursor = db.query("PRAGMA table_info($table)")
            var exists = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == column) {
                    exists = true
                    break
                }
            }
            cursor.close()
            return exists
        }

        private fun rebuildAssetAllocationEventsTable(db: SupportSQLiteDatabase) {
            if (!tableHasColumn(db, "asset_allocation_events", "allocationId")) return

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `asset_allocation_events_new` (
                    `allocationId` TEXT NOT NULL,
                    `assetId` TEXT NOT NULL,
                    `scopeType` TEXT NOT NULL,
                    `scopeId` TEXT NOT NULL,
                    `periodKey` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `syncStateInt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL,
                    `deleted` INTEGER NOT NULL,
                    `pendingSync` INTEGER NOT NULL,
                    `syncState` TEXT NOT NULL,
                    `syncError` TEXT,
                    `localUpdatedAt` INTEGER NOT NULL,
                    `serverUpdatedAt` INTEGER,
                    `cloudId` TEXT NOT NULL,
                    PRIMARY KEY(`allocationId`)
                )
            """)

            val syncStateIntExpr = if (tableHasColumn(db, "asset_allocation_events", "syncStateInt")) "`syncStateInt`" else "0"
            val deletedExpr = if (tableHasColumn(db, "asset_allocation_events", "deleted")) "`deleted`" else "CASE WHEN $syncStateIntExpr = -1 THEN 1 ELSE 0 END"
            val pendingSyncExpr = if (tableHasColumn(db, "asset_allocation_events", "pendingSync")) "`pendingSync`" else "CASE WHEN $syncStateIntExpr = 0 THEN 0 ELSE 1 END"
            val syncStateExpr = if (tableHasColumn(db, "asset_allocation_events", "syncState")) "`syncState`" else "CASE WHEN $syncStateIntExpr = 0 THEN 'SYNCED' ELSE 'PENDING' END"
            val syncErrorExpr = if (tableHasColumn(db, "asset_allocation_events", "syncError")) "`syncError`" else "NULL"
            val localUpdatedExpr = if (tableHasColumn(db, "asset_allocation_events", "localUpdatedAt")) "`localUpdatedAt`" else "`createdAt`"
            val serverUpdatedExpr = if (tableHasColumn(db, "asset_allocation_events", "serverUpdatedAt")) "`serverUpdatedAt`" else "NULL"
            val cloudIdExpr = if (tableHasColumn(db, "asset_allocation_events", "cloudId")) "`cloudId`" else "`allocationId`"
            val syncIdExpr = if (tableHasColumn(db, "asset_allocation_events", "syncId")) "`syncId`" else "`allocationId`"

            db.execSQL("""
                INSERT INTO `asset_allocation_events_new` (
                    `allocationId`, `assetId`, `scopeType`, `scopeId`, `periodKey`, `amount`,
                    `ownerUserId`, `syncStateInt`, `createdAt`, `syncId`, `deleted`, `pendingSync`,
                    `syncState`, `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                )
                SELECT
                    `allocationId`, `assetId`, `scopeType`, `scopeId`, `periodKey`, `amount`,
                    `ownerUserId`, $syncStateIntExpr, `createdAt`, $syncIdExpr, $deletedExpr, $pendingSyncExpr,
                    $syncStateExpr, $syncErrorExpr, $localUpdatedExpr, $serverUpdatedExpr, $cloudIdExpr
                FROM `asset_allocation_events`
            """)

            db.execSQL("DROP TABLE `asset_allocation_events`")
            db.execSQL("ALTER TABLE `asset_allocation_events_new` RENAME TO `asset_allocation_events`")
        }

        private fun rebuildAssetsTable(db: SupportSQLiteDatabase) {
            if (!tableHasColumn(db, "assets", "assetId")) return

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `assets_new` (
                    `assetId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `linkedDeviceId` TEXT,
                    `purchaseDateEpochMs` INTEGER NOT NULL,
                    `purchasePrice` REAL NOT NULL,
                    `residualValue` REAL NOT NULL,
                    `depreciationMethod` TEXT NOT NULL,
                    `usefulLifeMonths` INTEGER,
                    `expectedCycles` INTEGER,
                    `cyclesAllocatedCount` INTEGER NOT NULL,
                    `lastAllocatedAtEpochMs` INTEGER,
                    `retiredDateEpochMs` INTEGER,
                    `retirementValue` REAL,
                    `status` TEXT NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `syncStateInt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL,
                    `deleted` INTEGER NOT NULL,
                    `pendingSync` INTEGER NOT NULL,
                    `syncState` TEXT NOT NULL,
                    `syncError` TEXT,
                    `localUpdatedAt` INTEGER NOT NULL,
                    `serverUpdatedAt` INTEGER,
                    `cloudId` TEXT NOT NULL,
                    PRIMARY KEY(`assetId`)
                )
            """)

            val syncStateIntExpr = if (tableHasColumn(db, "assets", "syncStateInt")) "`syncStateInt`" else "0"
            val updatedAtExpr = if (tableHasColumn(db, "assets", "updatedAt")) "`updatedAt`" else "COALESCE(`localUpdatedAt`, `purchaseDateEpochMs`)"
            val syncIdExpr = if (tableHasColumn(db, "assets", "syncId")) "`syncId`" else "`assetId`"
            val deletedExpr = if (tableHasColumn(db, "assets", "deleted")) "`deleted`" else "CASE WHEN $syncStateIntExpr = -1 THEN 1 ELSE 0 END"
            val pendingSyncExpr = if (tableHasColumn(db, "assets", "pendingSync")) "`pendingSync`" else "CASE WHEN $syncStateIntExpr = 0 THEN 0 ELSE 1 END"
            val syncStateExpr = if (tableHasColumn(db, "assets", "syncState")) "`syncState`" else "CASE WHEN $syncStateIntExpr = 0 THEN 'SYNCED' ELSE 'PENDING' END"
            val syncErrorExpr = if (tableHasColumn(db, "assets", "syncError")) "`syncError`" else "NULL"
            val localUpdatedExpr = if (tableHasColumn(db, "assets", "localUpdatedAt")) "`localUpdatedAt`" else updatedAtExpr
            val serverUpdatedExpr = if (tableHasColumn(db, "assets", "serverUpdatedAt")) "`serverUpdatedAt`" else "NULL"
            val cloudIdExpr = if (tableHasColumn(db, "assets", "cloudId")) "`cloudId`" else "`assetId`"

            db.execSQL("""
                INSERT INTO `assets_new` (
                    `assetId`, `name`, `category`, `linkedDeviceId`, `purchaseDateEpochMs`, `purchasePrice`,
                    `residualValue`, `depreciationMethod`, `usefulLifeMonths`, `expectedCycles`,
                    `cyclesAllocatedCount`, `lastAllocatedAtEpochMs`, `retiredDateEpochMs`, `retirementValue`,
                    `status`, `ownerUserId`, `syncStateInt`, `updatedAt`, `syncId`, `deleted`, `pendingSync`,
                    `syncState`, `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                )
                SELECT
                    `assetId`, `name`, `category`, `linkedDeviceId`, `purchaseDateEpochMs`, `purchasePrice`,
                    `residualValue`, `depreciationMethod`, `usefulLifeMonths`, `expectedCycles`,
                    `cyclesAllocatedCount`, `lastAllocatedAtEpochMs`, `retiredDateEpochMs`, `retirementValue`,
                    `status`, `ownerUserId`, $syncStateIntExpr, $updatedAtExpr, $syncIdExpr, $deletedExpr, $pendingSyncExpr,
                    $syncStateExpr, $syncErrorExpr, $localUpdatedExpr, $serverUpdatedExpr, $cloudIdExpr
                FROM `assets`
            """)

            db.execSQL("DROP TABLE `assets`")
            db.execSQL("ALTER TABLE `assets_new` RENAME TO `assets`")
        }

        private fun rebuildCostBasisLedgerTable(db: SupportSQLiteDatabase) {
            if (!tableHasColumn(db, "cost_basis_ledger", "entryId")) return

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `cost_basis_ledger_new` (
                    `entryId` TEXT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `sourceType` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `syncStateInt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `syncId` TEXT NOT NULL,
                    `deleted` INTEGER NOT NULL,
                    `pendingSync` INTEGER NOT NULL,
                    `syncState` TEXT NOT NULL,
                    `syncError` TEXT,
                    `localUpdatedAt` INTEGER NOT NULL,
                    `serverUpdatedAt` INTEGER,
                    `cloudId` TEXT NOT NULL,
                    PRIMARY KEY(`entryId`)
                )
            """)

            val syncStateIntExpr = if (tableHasColumn(db, "cost_basis_ledger", "syncStateInt")) "`syncStateInt`" else "0"
            val deletedExpr = if (tableHasColumn(db, "cost_basis_ledger", "deleted")) "`deleted`" else "CASE WHEN $syncStateIntExpr = -1 THEN 1 ELSE 0 END"
            val pendingSyncExpr = if (tableHasColumn(db, "cost_basis_ledger", "pendingSync")) "`pendingSync`" else "CASE WHEN $syncStateIntExpr = 0 THEN 0 ELSE 1 END"
            val syncStateExpr = if (tableHasColumn(db, "cost_basis_ledger", "syncState")) "`syncState`" else "CASE WHEN $syncStateIntExpr = 0 THEN 'SYNCED' ELSE 'PENDING' END"
            val syncErrorExpr = if (tableHasColumn(db, "cost_basis_ledger", "syncError")) "`syncError`" else "NULL"
            val localUpdatedExpr = if (tableHasColumn(db, "cost_basis_ledger", "localUpdatedAt")) "`localUpdatedAt`" else "`createdAt`"
            val serverUpdatedExpr = if (tableHasColumn(db, "cost_basis_ledger", "serverUpdatedAt")) "`serverUpdatedAt`" else "NULL"
            val cloudIdExpr = if (tableHasColumn(db, "cost_basis_ledger", "cloudId")) "`cloudId`" else "`entryId`"
            val syncIdExpr = if (tableHasColumn(db, "cost_basis_ledger", "syncId")) "`syncId`" else "`entryId`"

            db.execSQL("""
                INSERT INTO `cost_basis_ledger_new` (
                    `entryId`, `entityType`, `entityId`, `sourceType`, `amount`, `ownerUserId`,
                    `syncStateInt`, `createdAt`, `syncId`, `deleted`, `pendingSync`, `syncState`,
                    `syncError`, `localUpdatedAt`, `serverUpdatedAt`, `cloudId`
                )
                SELECT
                    `entryId`, `entityType`, `entityId`, `sourceType`, `amount`, `ownerUserId`,
                    $syncStateIntExpr, `createdAt`, $syncIdExpr, $deletedExpr, $pendingSyncExpr, $syncStateExpr,
                    $syncErrorExpr, $localUpdatedExpr, $serverUpdatedExpr, $cloudIdExpr
                FROM `cost_basis_ledger`
            """)

            db.execSQL("DROP TABLE `cost_basis_ledger`")
            db.execSQL("ALTER TABLE `cost_basis_ledger_new` RENAME TO `cost_basis_ledger`")
        }

        private fun rebuildBreedingActionPlansTable(db: SupportSQLiteDatabase) {
            if (!tableHasColumn(db, "breeding_action_plans", "syncId")) return

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `breeding_action_plans_new` (
                    `syncId` TEXT NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `scenarioId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `steps` TEXT NOT NULL,
                    `lastModified` INTEGER NOT NULL,
                    `syncState` TEXT NOT NULL,
                    `syncError` TEXT,
                    `cloudUpdatedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    `linkedAssets` TEXT NOT NULL,
                    `planSpecies` TEXT NOT NULL,
                    `finalGeneration` INTEGER NOT NULL,
                    `activeGenerationIndex` INTEGER NOT NULL,
                    `mergeMode` TEXT NOT NULL,
                    `auditLog` TEXT NOT NULL,
                    `entryMode` TEXT NOT NULL,
                    `target` TEXT,
                    `summaryRationale` TEXT,
                    PRIMARY KEY(`syncId`)
                )
            """)

            val linkedAssetsExpr = if (tableHasColumn(db, "breeding_action_plans", "linkedAssets")) "`linkedAssets`" else "'[]'"
            val planSpeciesExpr = if (tableHasColumn(db, "breeding_action_plans", "planSpecies")) "`planSpecies`" else "'CHICKEN'"
            val finalGenerationExpr = if (tableHasColumn(db, "breeding_action_plans", "finalGeneration")) "`finalGeneration`" else "1"
            val activeGenerationIndexExpr = if (tableHasColumn(db, "breeding_action_plans", "activeGenerationIndex")) "`activeGenerationIndex`" else "1"
            val mergeModeExpr = if (tableHasColumn(db, "breeding_action_plans", "mergeMode")) "`mergeMode`" else "'KEEP_SEPARATE'"
            val auditLogExpr = if (tableHasColumn(db, "breeding_action_plans", "auditLog")) "`auditLog`" else "'[]'"
            val entryModeExpr = if (tableHasColumn(db, "breeding_action_plans", "entryMode")) "`entryMode`" else "'FORWARD'"
            val targetExpr = if (tableHasColumn(db, "breeding_action_plans", "target")) "`target`" else "NULL"
            val summaryRationaleExpr = if (tableHasColumn(db, "breeding_action_plans", "summaryRationale")) "`summaryRationale`" else "NULL"

            db.execSQL("""
                INSERT INTO `breeding_action_plans_new` (
                    `syncId`, `ownerUserId`, `scenarioId`, `name`, `createdAt`, `updatedAt`,
                    `status`, `steps`, `lastModified`, `syncState`, `syncError`, `cloudUpdatedAt`,
                    `deleted`, `linkedAssets`, `planSpecies`, `finalGeneration`, `activeGenerationIndex`,
                    `mergeMode`, `auditLog`, `entryMode`, `target`, `summaryRationale`
                )
                SELECT
                    `syncId`, `ownerUserId`, `scenarioId`, `name`, `createdAt`, `updatedAt`,
                    `status`, `steps`, `lastModified`, `syncState`, `syncError`, `cloudUpdatedAt`,
                    `deleted`, $linkedAssetsExpr, $planSpeciesExpr, $finalGenerationExpr, $activeGenerationIndexExpr,
                    $mergeModeExpr, $auditLogExpr, $entryModeExpr, $targetExpr, $summaryRationaleExpr
                FROM `breeding_action_plans`
            """)

            db.execSQL("DROP TABLE `breeding_action_plans`")
            db.execSQL("ALTER TABLE `breeding_action_plans_new` RENAME TO `breeding_action_plans`")
        }
    }
}
