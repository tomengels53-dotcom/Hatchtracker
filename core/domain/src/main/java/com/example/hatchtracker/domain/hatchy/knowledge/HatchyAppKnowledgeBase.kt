package com.example.hatchtracker.domain.hatchy.knowledge

import com.example.hatchtracker.domain.hatchy.model.HatchyModule

/**
 * Canonical knowledge base for HatchBase's application structure and navigation.
 */
data class ModuleHint(
    val moduleKey: HatchyModule,
    val displayName: String,
    val entryRoutes: List<String>,
    val primaryActions: List<String>,
    val commonIssues: List<String>,
    val lifecycleImpact: String? = null
)

object HatchyAppKnowledgeBase {

    val ModuleMap: Map<HatchyModule, ModuleHint> = mapOf(
        HatchyModule.HOME to ModuleHint(
            moduleKey = HatchyModule.HOME,
            displayName = "Dashboard",
            entryRoutes = listOf("welcome", "main_menu/home"),
            primaryActions = listOf("View active counts", "Check notifications", "Access help chat"),
            commonIssues = listOf("Data not refreshing", "Missing dashboard cards")
        ),
        HatchyModule.FLOCK to ModuleHint(
            moduleKey = HatchyModule.FLOCK,
            displayName = "Flock Management",
            entryRoutes = listOf("flock_list", "add_flock"),
            primaryActions = listOf("Add New Flock", "Record Bird (Ring #)", "Update Status", "Collect Eggs"),
            commonIssues = listOf("Bird not appearing in list", "Incorrect bird status", "Ring number duplicate"),
            lifecycleImpact = "Maintains adult birds. Hatch Date is optional if unknown."
        ),
        HatchyModule.INCUBATION to ModuleHint(
            moduleKey = HatchyModule.INCUBATION,
            displayName = "Incubation",
            entryRoutes = listOf("incubation_list", "add_incubation", "hatch_planner"),
            primaryActions = listOf("Start New Incubation", "Plan Hatches (Calendar)", "Log Candling", "Record Hatch Outcome"),
            commonIssues = listOf("Timer not updating", "Hatch date calculation errors"),
            lifecycleImpact = "Transforms collected eggs into live hatchouts. Use 'Hatch Planner' to schedule."
        ),
        HatchyModule.NURSERY to ModuleHint(
            moduleKey = HatchyModule.NURSERY,
            displayName = "Nursery (Brooder)",
            entryRoutes = listOf("nursery"),
            primaryActions = listOf("Create Flocklet Batch", "Monitor Brooder Temps", "Move to Flock"),
            commonIssues = listOf("Batch size mismatch", "Moving to flock failed"),
            lifecycleImpact = "Houses chicks from day-old until they are ready to join the main flock."
        ),
        HatchyModule.FINANCE to ModuleHint(
            moduleKey = HatchyModule.FINANCE,
            displayName = "Financials",
            entryRoutes = listOf("financial_stats", "add_financial_entry", "add_sales_batch"),
            primaryActions = listOf("Record Expense", "Record Sales Batch", "View Profit/Loss", "Currency Settings"),
            commonIssues = listOf("Category mapping errors", "Missing sale records"),
            lifecycleImpact = "Tracks value. 'Sales Batches' let you sell multiple birds/eggs at once."
        ),
        HatchyModule.BREEDING to ModuleHint(
            moduleKey = HatchyModule.BREEDING,
            displayName = "Breeding & Genetics (PRO)",
            entryRoutes = listOf("breeding", "breeding/scenarios", "breeding/actionplans"),
            primaryActions = listOf("Create Scenario (Wizard)", "Simulate Generations", "Convert to Action Plan", "Track Lineage"),
            commonIssues = listOf("PRO subscription required", "Scenario conversion limits"),
            lifecycleImpact = "Plans future generations (Scenarios) and tracks execution (Action Plans) to optimize flock genetics."
        ),
        HatchyModule.SUPPORT to ModuleHint(
            moduleKey = HatchyModule.SUPPORT,
            displayName = "Help & Support",
            entryRoutes = listOf("help_support", "hatchy_chat", "user_ticket_detail"),
            primaryActions = listOf("Chat with Hatchy", "Open Support Ticket", "Check Ticket Status"),
            commonIssues = listOf("Response delays", "Attachment upload failure")
        ),
        HatchyModule.ADMIN to ModuleHint(
            moduleKey = HatchyModule.ADMIN,
            displayName = "Admin Hub",
            entryRoutes = listOf("admin_menu", "admin_ticket_dashboard", "breed_admin"),
            primaryActions = listOf("Manage Breed Standards", "Triage Tickets", "View Audit Logs"),
            commonIssues = listOf("Permissions denied", "Dashboard sync lag")
        )
    )

    fun getNavResponse(module: HatchyModule): String {
        val hint = ModuleMap[module] ?: return "I'm happy to help you find your way. Tell me what you're lookin' for: Flock, Incubation, or maybe the Nursery?"
        
        val breedTip = if (module == HatchyModule.BREEDING) "\n🐣 Need to select a breed? You can pick a specific breed when adding a new Flock, setting up a new Incubation, logging manual chicks in the Nursery, or pickin' targets in the Strategy Planner." else ""

        return "To get things movin' in the ${hint.displayName} module, here's the path:\n\n" +
               "📍 Entry: ${hint.entryRoutes.firstOrNull() ?: "Standard Navigation"}\n" +
               "🛠️ Actions: ${hint.primaryActions.joinToString(", ")}\n" +
               "💡 Tip: ${hint.lifecycleImpact ?: "This is where you keep your records straight."}$breedTip"
    }
}
