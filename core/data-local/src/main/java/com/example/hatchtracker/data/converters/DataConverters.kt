package com.example.hatchtracker.data.converters

import androidx.room.TypeConverter
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.data.models.ScenarioStatus
import com.example.hatchtracker.data.models.ScenarioTraitConfig
import com.example.hatchtracker.data.models.ScenarioGeneration
import com.example.hatchtracker.data.models.ScenarioRisk
import com.example.hatchtracker.data.models.BreedingProgramStatus
import com.example.hatchtracker.data.models.BreedingProgramStep
import com.example.hatchtracker.data.models.BreedingProgramAssetLink
import com.example.hatchtracker.data.models.BreedingProgramAuditEntry
import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.data.models.BirdTraitOverride
import com.example.hatchtracker.model.BirdLifecycleStage
import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.model.GeneticProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataConverters {
    private val gson = Gson()
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return gson.toJson(value ?: emptyMap<String, String>())
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromFloatMap(value: Map<String, Float>?): String {
        return gson.toJson(value ?: emptyMap<String, Float>())
    }

    @TypeConverter
    fun toFloatMap(value: String?): Map<String, Float> {
        if (value.isNullOrBlank()) return emptyMap()
        val mapType = object : TypeToken<Map<String, Float>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    @TypeConverter
    fun fromSex(value: Sex): String {
        return value.name
    }

    @TypeConverter
    fun toSex(value: String): Sex {
        return try {
            Sex.valueOf(value.uppercase())
        } catch (e: Exception) {
            Sex.UNKNOWN
        }
    }

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String {
        return value.name
    }

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel {
        return try {
            ConfidenceLevel.valueOf(value.uppercase())
        } catch (e: Exception) {
            ConfidenceLevel.LOW
        }
    }

    @TypeConverter
    fun fromSyncState(value: SyncState): String {
        return value.name
    }

    @TypeConverter
    fun toSyncState(value: String): SyncState {
        return try {
            SyncState.valueOf(value.uppercase())
        } catch (e: Exception) {
            SyncState.SYNCED
        }
    }

    @TypeConverter
    fun fromScenarioStatus(value: ScenarioStatus): String = value.name

    @TypeConverter
    fun toScenarioStatus(value: String): ScenarioStatus = try {
        ScenarioStatus.valueOf(value.uppercase())
    } catch (e: Exception) {
        ScenarioStatus.DRAFT
    }

    @TypeConverter
    fun fromActionPlanStatus(value: BreedingProgramStatus): String = value.name

    @TypeConverter
    fun toActionPlanStatus(value: String): BreedingProgramStatus = try {
        BreedingProgramStatus.valueOf(value.uppercase())
    } catch (e: Exception) {
        BreedingProgramStatus.ACTIVE
    }

    @TypeConverter
    fun fromScenarioTraitPriorities(value: Map<String, ScenarioTraitConfig>?): String = gson.toJson(value ?: emptyMap<String, ScenarioTraitConfig>())

    @TypeConverter
    fun toScenarioTraitPriorities(value: String?): Map<String, ScenarioTraitConfig> {
        if (value.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, ScenarioTraitConfig>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromScenarioGenerations(value: List<ScenarioGeneration>?): String = gson.toJson(value ?: emptyList<ScenarioGeneration>())

    @TypeConverter
    fun toScenarioGenerations(value: String?): List<ScenarioGeneration> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<ScenarioGeneration>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromScenarioRisks(value: List<ScenarioRisk>?): String = gson.toJson(value ?: emptyList<ScenarioRisk>())

    @TypeConverter
    fun toScenarioRisks(value: String?): List<ScenarioRisk> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<ScenarioRisk>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromActionPlanSteps(value: List<BreedingProgramStep>?): String =
        gson.toJson(value ?: emptyList<BreedingProgramStep>())

    @TypeConverter
    fun toActionPlanSteps(value: String?): List<BreedingProgramStep> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<BreedingProgramStep>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBirdTraitOverrides(value: List<BirdTraitOverride>?): String =
        gson.toJson(value ?: emptyList<BirdTraitOverride>())

    @TypeConverter
    fun toBirdTraitOverrides(value: String?): List<BirdTraitOverride> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<BirdTraitOverride>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBreedingProgramAssetLinks(value: List<BreedingProgramAssetLink>?): String =
        gson.toJson(value ?: emptyList<BreedingProgramAssetLink>())

    @TypeConverter
    fun toBreedingProgramAssetLinks(value: String?): List<BreedingProgramAssetLink> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<BreedingProgramAssetLink>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBreedingProgramAuditEntries(value: List<BreedingProgramAuditEntry>?): String =
        gson.toJson(value ?: emptyList<BreedingProgramAuditEntry>())

    @TypeConverter
    fun toBreedingProgramAuditEntries(value: String?): List<BreedingProgramAuditEntry> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<BreedingProgramAuditEntry>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBreedingTarget(value: BreedingTarget?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBreedingTarget(value: String?): BreedingTarget? {
        if (value.isNullOrBlank()) return null
        return gson.fromJson(value, BreedingTarget::class.java)
    }

    @TypeConverter
    fun fromGenotypeCallMap(value: Map<String, GenotypeCall>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toGenotypeCallMap(value: String?): Map<String, GenotypeCall>? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<Map<String, GenotypeCall>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromLifecycleStage(value: BirdLifecycleStage): String = value.name

    @TypeConverter
    fun toLifecycleStage(value: String): BirdLifecycleStage = try {
        BirdLifecycleStage.valueOf(value.uppercase())
    } catch (e: Exception) {
        BirdLifecycleStage.ADULT
    }

    @TypeConverter
    fun fromGeneticProfile(value: GeneticProfile?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toGeneticProfile(value: String?): GeneticProfile? {
        if (value.isNullOrBlank()) return null
        return gson.fromJson(value, GeneticProfile::class.java)
    }

    @TypeConverter
    fun fromQuantitativeTraits(value: Map<String, QuantitativeTraitValue>?): String {
        return gson.toJson(value ?: emptyMap<String, QuantitativeTraitValue>())
    }

    @TypeConverter
    fun toQuantitativeTraits(value: String?): Map<String, QuantitativeTraitValue> {
        if (value.isNullOrBlank() || value == "{}") return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, QuantitativeTraitValue>>() {}.type
            gson.fromJson(value, type) ?: emptyMap()
        } catch (e: Exception) {
            println("DataConverters: Error parsing QuantitativeTraitValue map: ${e.message}")
            emptyMap()
        }
    }

    @TypeConverter
    fun fromBreedContributionList(value: List<BreedContribution>?): String {
        return gson.toJson(value ?: emptyList<BreedContribution>())
    }

    @TypeConverter
    fun toBreedContributionList(value: String?): List<BreedContribution> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<BreedContribution>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
