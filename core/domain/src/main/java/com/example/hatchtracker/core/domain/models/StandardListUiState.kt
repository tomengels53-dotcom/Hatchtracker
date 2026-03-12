package com.example.hatchtracker.core.domain.models

import com.example.hatchtracker.model.UiText

/**
 * Standardized Sectioning for Lists across features
 */
enum class ListSection(val priority: Int, val titleKey: String) {
    CRITICAL(0, "section_critical"),
    NEXT_7_DAYS(1, "section_next_7_days"),
    STABLE(2, "section_stable"),
    ARCHIVED(3, "section_archived")
}

/**
 * Core interface that all List Row Models (DTOs) must implement for symmetric grouping.
 */
interface StandardRowModel {
    val id: String
    val urgencyScore: Int
    val isCompletedOrArchived: Boolean
    val dueToday: Boolean
    val dueWithin7Days: Boolean
}

/**
 * Standard sectioning algorithm to prevent feature drift.
 */
fun <T : StandardRowModel> groupByStandardSection(items: List<T>, criticalThreshold: Int = 80): Map<ListSection, List<T>> {
    val grouped = mutableMapOf<ListSection, MutableList<T>>(
        ListSection.CRITICAL to mutableListOf(),
        ListSection.NEXT_7_DAYS to mutableListOf(),
        ListSection.STABLE to mutableListOf(),
        ListSection.ARCHIVED to mutableListOf()
    )

    for (item in items) {
        when {
            item.isCompletedOrArchived -> grouped[ListSection.ARCHIVED]?.add(item)
            item.urgencyScore >= criticalThreshold || item.dueToday -> grouped[ListSection.CRITICAL]?.add(item)
            item.dueWithin7Days -> grouped[ListSection.NEXT_7_DAYS]?.add(item)
            else -> grouped[ListSection.STABLE]?.add(item)
        }
    }

    return grouped
}

/**
 * Standardized State Contract for all list-based UI screens.
 */
data class StandardListUiState<T : StandardRowModel>(
    val items: List<T> = emptyList(),
    val groupedItems: Map<ListSection, List<T>> = emptyMap(),
    val filter: String = "All", // Represents current generic filter selection
    val sortMode: String = "Urgency",
    val searchQuery: String = "",
    val collapsedSections: Set<ListSection> = setOf(ListSection.ARCHIVED), // Archived collapsed by default
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)
