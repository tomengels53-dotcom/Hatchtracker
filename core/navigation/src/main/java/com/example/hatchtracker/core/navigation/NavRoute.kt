package com.example.hatchtracker.core.navigation

sealed class NavRoute(val route: String) {
    object Welcome : NavRoute("welcome")
    object Login : NavRoute("login")
    object SignUp : NavRoute("signup")
    object Onboarding : NavRoute("onboarding")

    object MainMenu : NavRoute("main_menu/{moduleName}") {
        fun createRoute(moduleName: String) = "main_menu/$moduleName"
    }

    // Bird Module
    object BirdList : NavRoute("bird_list")
    object BirdDetail : NavRoute("bird_detail/{birdId}") {
        fun createRoute(birdId: Long) = "bird_detail/$birdId"
    }
    object AddBird : NavRoute("add_bird?flockId={flockId}&species={species}") {
        fun createRoute(flockId: Long? = null, species: String? = null): String {
            val base = "add_bird"
            val params = mutableListOf<String>()
            if (flockId != null) params.add("flockId=$flockId")
            if (species != null) params.add("species=$species")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object BreedSelection : NavRoute("breed_selection/{speciesId}") {
        fun createRoute(speciesId: String) = "breed_selection/$speciesId"
    }
    object BreedDetail : NavRoute("breed_detail/{breedId}") {
        fun createRoute(breedId: String) = "breed_detail/$breedId"
    }

    // Flock Module
    object FlockList : NavRoute("flock_list")
    object FlockDetail : NavRoute("flock_detail/{flockId}") {
        fun createRoute(flockId: Long) = "flock_detail/$flockId"
    }
    object FlockletDetail : NavRoute("flocklet_detail/{flockletId}") {
        fun createRoute(flockletId: Long) = "flocklet_detail/$flockletId"
    }
    object AddFlock : NavRoute("add_flock?species={species}&prefilledBreeds={prefilledBreeds}&graduatingFlockletId={graduatingFlockletId}") {
        fun createRoute(species: String? = null, prefilledBreeds: List<String>? = null, graduatingFlockletId: Long? = null): String {
            val base = "add_flock"
            val params = mutableListOf<String>()
            if (species != null) params.add("species=$species")
            if (!prefilledBreeds.isNullOrEmpty()) params.add("prefilledBreeds=${prefilledBreeds.joinToString(",")}")
            if (graduatingFlockletId != null) params.add("graduatingFlockletId=$graduatingFlockletId")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object AddDevice : NavRoute("add_device")

    // Incubation Module
    object IncubationList : NavRoute("incubation_list")
    object IncubationDetail : NavRoute("incubation_detail/{incubationId}") {
        fun createRoute(incubationId: Long) = "incubation_detail/$incubationId"
    }
    object AddIncubation : NavRoute("add_incubation?flockId={flockId}&species={species}") {
        fun createRoute(flockId: Long? = null, species: String? = null): String {
            val base = "add_incubation"
            val params = mutableListOf<String>()
            if (flockId != null) params.add("flockId=$flockId")
            if (species != null) params.add("species=$species")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object HatchPlanner : NavRoute("hatch_planner")
    object HatchOutcome : NavRoute("hatch_outcome/{incubationId}") {
        fun createRoute(incubationId: Long) = "hatch_outcome/$incubationId"
    }
    object HatchSummary : NavRoute("hatch_summary/{incubationId}") {
        fun createRoute(incubationId: Long) = "hatch_summary/$incubationId"
    }
    object IncubationTimeline : NavRoute("incubation_timeline/{incubationId}") {
        fun createRoute(incubationId: Long) = "incubation_timeline/$incubationId"
    }

    // Nursery Module
    object Nursery : NavRoute("nursery")

    object Breeding : NavRoute("breeding")
    object BreedSearchInsights : NavRoute("breeding/breed_search_insights")
    object BreedingPrograms : NavRoute("breeding/programs")
    object BreedingProgramDetail : NavRoute("breeding/program/{planId}") {
        fun createRoute(planId: String) = "breeding/program/$planId"
    }
    object BreedingScenario : NavRoute("breeding_scenario")
    object BreedingHistory : NavRoute("breeding_history")
    object BreedingLocked : NavRoute("breeding_locked")
    object CommunityValidation : NavRoute("community_validation")
    object MultiFlockOptimization : NavRoute("multi_flock_optimization")
    object BreedingProgramWizard : NavRoute("breeding_program_wizard")
    object TraitObservation : NavRoute("trait_observation?breedId={breedId}&parentPairId={parentPairId}&traitId={traitId}") {
        fun createRoute(breedId: String? = null, parentPairId: String? = null, traitId: String? = null): String {
            val base = "trait_observation"
            val params = mutableListOf<String>()
            if (!breedId.isNullOrBlank()) params.add("breedId=$breedId")
            if (!parentPairId.isNullOrBlank()) params.add("parentPairId=$parentPairId")
            if (!traitId.isNullOrBlank()) params.add("traitId=$traitId")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object TraitPromotion : NavRoute("trait_promotion")
    
    // Admin Module
    object AdminMenu : NavRoute("admin_menu")
    object AdminAuditLog : NavRoute("admin_audit_log")
    object AdminTicketDashboard : NavRoute("admin_ticket_dashboard")
    object AdminTicketDetail : NavRoute("admin_ticket_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "admin_ticket_detail/$ticketId"
    }
    object BreedAdmin : NavRoute("breed_admin")

    // Financial Module
    object FinancialStats : NavRoute("financial_stats")
    object AddFinancialEntry : NavRoute("add_financial_entry/{ownerId}/{ownerType}/{isRevenue}") {
        fun createRoute(ownerId: String, ownerType: String, isRevenue: Boolean) = "add_financial_entry/$ownerId/$ownerType/$isRevenue"
    }
    object FinancialTransactions : NavRoute("financial_transactions/{ownerId}/{ownerType}") {
        fun createRoute(ownerId: String, ownerType: String) = "financial_transactions/$ownerId/$ownerType"
    }
    object AddSalesBatch : NavRoute("add_sales_batch/{ownerId}/{ownerType}?birdIds={birdIds}") {
        fun createRoute(ownerId: String, ownerType: String, birdIds: List<Long>? = null): String {
            val base = "add_sales_batch/$ownerId/$ownerType"
            return if (birdIds.isNullOrEmpty()) base else "$base?birdIds=${birdIds.joinToString(",")}"
        }
    }

    object EggProduction : NavRoute("egg_production?flockId={flockId}") {
        fun createRoute(flockId: String? = null): String {
             return if (flockId != null) "egg_production?flockId=$flockId" else "egg_production"
        }
    }

    // Misc
    object Paywall : NavRoute("paywall")
    object Troubleshooting : NavRoute("troubleshooting/{incubationId}") {
        fun createRoute(incubationId: Long) = "troubleshooting/$incubationId"
    }
    object NotificationCenter : NavRoute("notification_center")
    object CommunityDevTools : NavRoute("community_dev_tools")
    
    // Moderation Module
    object ModerationQueue : NavRoute("moderation/queue")
    object ReportDetail : NavRoute("moderation/report/{reportId}") {
        fun createRoute(reportId: String) = "moderation/report/$reportId"
    }
    object UserSafety : NavRoute("moderation/user/{userId}") {
        fun createRoute(userId: String) = "moderation/user/$userId"
    }
    object ListingModerationReview : NavRoute("moderation/listing/{listingId}") {
        fun createRoute(listingId: String) = "moderation/listing/$listingId"
    }

    // Account & Support
    object UserProfile : NavRoute("user_profile")
    object ProfileSetup : NavRoute("profile_setup")
    object HelpSupport : NavRoute("help_support?moduleId={moduleId}&featureId={featureId}") {
        fun createRoute(moduleId: String? = null, featureId: String? = null): String {
            val base = "help_support"
            val params = mutableListOf<String>()
            if (moduleId != null) params.add("moduleId=$moduleId")
            if (featureId != null) params.add("featureId=$featureId")
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }
    object HatchyChat : NavRoute("hatchy_chat")
    object UserTicketDetail : NavRoute("user_ticket_detail/{ticketId}") {
        fun createRoute(ticketId: String) = "user_ticket_detail/$ticketId"
    }

    // Scanner Suite
    object ScanRing : NavRoute("scan/ring/{birdId}") {
        fun createRoute(birdId: Long? = null): String {
            return if (birdId != null) "scan/ring/$birdId" else "scan/ring/-1"
        }
    }
    object ScanIncubator : NavRoute("scan/incubator/{incubationId}") {
        fun createRoute(incubationId: Long) = "scan/incubator/$incubationId"
    }
    object ScanReceipt : NavRoute("scan/receipt/{ownerId}/{ownerType}") {
        fun createRoute(ownerId: String, ownerType: String) = "scan/receipt/$ownerId/$ownerType"
    }

    object Legal : NavRoute("legal/{docType}") {
        fun createRoute(docType: String) = "legal/$docType"
    }
}
