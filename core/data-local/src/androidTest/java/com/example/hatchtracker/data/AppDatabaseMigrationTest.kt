package com.example.hatchtracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Requires legacy Room schema assets (10.json/11.json) that are no longer present in this module.")
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate10To11_addsVatAndDepreciationFields() {
        // Create database with version 10
        val db = helper.createDatabase(TEST_DB, 10)
        
        // Seed some data in financial_entries
        db.execSQL("INSERT INTO financial_entries (ownerId, ownerType, type, category, amount, date) VALUES ('bird1', 'bird', 'cost', 'Feed', 100.0, 1700000000000)")

        db.close()

        // Migrate to version 11
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 11, true, AppDatabase.MIGRATION_10_11)

        // Verify columns
        val columns = readTableInfo(migratedDb, "financial_entries")
        assertNotNull(columns["amountNet"])
        assertNotNull(columns["amountVAT"])
        assertNotNull(columns["amountGross"])
        assertNotNull(columns["currency"])
        assertNotNull(columns["vatEnabled"])
        assertNotNull(columns["vatRate"])
        assertNotNull(columns["isRecurring"])
        assertNotNull(columns["recurrenceIntervalDays"])
        assertNotNull(columns["lastRecurrenceDate"])
        assertNotNull(columns["depreciationMonths"])

        // Verify backfill logic
        val cursor = migratedDb.query("SELECT * FROM financial_entries WHERE ownerId = 'bird1'")
        assertTrue(cursor.moveToFirst())
        assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amountNet")), 0.001)
        assertEquals(0.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amountVAT")), 0.001)
        assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amountGross")), 0.001)
        assertEquals("EUR", cursor.getString(cursor.getColumnIndexOrThrow("currency")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("vatEnabled")))
        cursor.close()
    }

    @Test
    fun migrate11To12_addsTraitOverrideColumns() {
        // Create database with version 11
        val db = helper.createDatabase(TEST_DB, 11)
        db.close()

        // Migrate to version 12
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 12, true, AppDatabase.MIGRATION_11_12)

        // Verify added columns in birds
        val birdColumns = readTableInfo(migratedDb, "birds")
        assertNotNull(birdColumns["genetic_traitOverrides"])
        assertNotNull(birdColumns["custom_traitOverrides"])
        assertTrue(birdColumns["genetic_traitOverrides"]?.notNull == true)

        // Verify added column in flocks
        val flockColumns = readTableInfo(migratedDb, "flocks")
        assertNotNull(flockColumns["default_traitOverrides"])
    }

    private fun readTableInfo(db: SupportSQLiteDatabase, table: String): Map<String, ColumnInfo> {
        val result = mutableMapOf<String, ColumnInfo>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                result[name] = ColumnInfo(
                    notNull = cursor.getInt(notNullIndex) == 1,
                    defaultValue = if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
                )
            }
        }
        return result
    }

    private data class ColumnInfo(
        val notNull: Boolean,
        val defaultValue: String?
    )

    private companion object {
        const val TEST_DB = "migration-test-db"
    }
}
