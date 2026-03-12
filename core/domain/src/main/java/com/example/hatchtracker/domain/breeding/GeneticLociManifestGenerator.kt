@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneticLociManifestGenerator @Inject constructor() {

    fun generateManifestJson(): String {
        val loci = GeneticLocusCatalog.loci.values.sortedBy { it.locusId }
        
        fun esc(value: String): String = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val lociJson = loci.joinToString(",\n    ") { locus ->
            val speciesName = if (locus.locusId == "SPECIES_Specific") {
                "ALL"
            } else if (locus.locusId.contains("__")) {
                locus.locusId.substringBefore("__")
            } else {
                "CHICKEN"
            }
            
            val allelesJson = locus.alleles.joinToString(",") { "\"${esc(it)}\"" }
            
            """{
      "locusId": "${esc(locus.locusId)}",
      "species": "$speciesName",
      "alleles": [$allelesJson],
      "defaultWildtype": "${esc(locus.defaultWildtype)}",
      "inheritance": "${locus.inheritance?.name ?: "UNKNOWN"}",
      "dominance": "${locus.dominance?.name ?: "UNKNOWN"}"
    }"""
        }

        val timestamp = try {
            java.time.Instant.now().toString()
        } catch (e: Exception) {
            "UNKNOWN_TIME"
        }

        return """{
  "generatedAt": "$timestamp",
  "loci": [
    $lociJson
  ]
}"""
    }
}
