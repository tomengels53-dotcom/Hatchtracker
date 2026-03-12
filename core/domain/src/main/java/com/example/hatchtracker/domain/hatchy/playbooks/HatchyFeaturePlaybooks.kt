package com.example.hatchtracker.domain.hatchy.playbooks

import com.example.hatchtracker.domain.hatchy.model.HatchyModule

/**
 * Step-by-step playbooks for core app workflows.
 */
data class Playbook(
    val startRoute: String,
    val workflow: List<String>,
    val commonMistakes: List<String>,
    val dataImpact: String
)

object HatchyFeaturePlaybooks {

    val FlockPlaybook = Playbook(
        startRoute = "add_flock",
        workflow = listOf(
            "1. Create a new flock (specify species/purpose)",
            "2. Add individual birds or starting batch",
            "3. Record daily egg collection",
            "4. Track health/feed events"
        ),
        commonMistakes = listOf("Adding birds without a flock", "Forgetting to record deceased birds"),
        dataImpact = "Populates the flock dashboard and enables egg inventory tracking."
    )

    val IncubationPlaybook = Playbook(
        startRoute = "add_incubation",
        workflow = listOf(
            "1. Select eggs from your inventory or external source",
            "2. Set incubator settings (temp/humidity)",
            "3. Track daily turning and candling (Day 7/14)",
            "4. Prepare for Lockdown 3 days before due date",
            "5. Record final Hatch Outcome"
        ),
        commonMistakes = listOf("Not stopping turning during lockdown", "Frequent lid opening"),
        dataImpact = "Generates batch records that eventually flow into the Nursery."
    )

    val NurseryPlaybook = Playbook(
        startRoute = "nursery",
        workflow = listOf(
            "1. Create flocklet batch from hatch outcome",
            "2. Set brooder temperature according to age",
            "3. Record growth milestones",
            "4. Graduate batch to Flock once feathered"
        ),
        commonMistakes = listOf("Keeping brooder too hot as they grow", "Mixing vastly different ages"),
        dataImpact = "Tracks the bridge between hatching and adult flock membership."
    )

    val FinancePlaybook = Playbook(
        startRoute = "financial_stats",
        workflow = listOf(
            "1. Record feed and equipment expenses",
            "2. Record bird sales linked to specific flocks/batches",
            "3. View monthly Profit/Loss report"
        ),
        commonMistakes = listOf("Mixing personal and flock expenses", "Not linking sales to the originating flock"),
        dataImpact = "Provides real-time ROI tracking for your poultry operation."
    )

    fun getPlaybookText(module: HatchyModule): String {
        val playbook = when(module) {
            HatchyModule.FLOCK -> FlockPlaybook
            HatchyModule.INCUBATION -> IncubationPlaybook
            HatchyModule.NURSERY -> NurseryPlaybook
            HatchyModule.FINANCE -> FinancePlaybook
            else -> return "I'm ready to walk you through any part of the yard. Tell me if it's Flock, Incubation, or Finance you're fixin' to work on."
        }

        return "Here's the plan for ${module.name.lowercase()}:\n\n" +
               "${playbook.workflow.joinToString("\n")}\n\n" +
               "⚠️ Watch out for: ${playbook.commonMistakes.joinToString(", ")}\n" +
               "📊 Logic: ${playbook.dataImpact}"
    }
}
