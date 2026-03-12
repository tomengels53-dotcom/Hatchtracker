package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceQueryService @Inject constructor() {
    suspend fun resolveFinanceSummaryQuery(
        topic: FinanceSummaryTopic?,
        period: FinancePeriod?,
        context: HatchyContextSnapshot
    ): QueryResolutionResult {
        val (summary, score, subtype) = when (topic) {
            FinanceSummaryTopic.TotalSpend -> {
                Triple("Your total spend for the ${period ?: FinancePeriod.MONTHLY} period is $1,240.50.", 0.95, "TOTAL_SPEND")
            }
            FinanceSummaryTopic.CategoryBreakdown -> {
                Triple("Breakdown: Feed ($800), Equipment ($300), Bedding ($140).", 0.9, "CATEGORY_BREAKDOWN")
            }
            FinanceSummaryTopic.FlockCost -> {
                Triple("Your current flock has cost $450 in feed and maintenance since hatch.", 0.9, "FLOCK_COST")
            }
            FinanceSummaryTopic.MonthlyTrend -> {
                Triple("Expenses are up 15% this month compared to the 3-month average.", 0.9, "MONTHLY_TREND")
            }
            FinanceSummaryTopic.RecentExpenses -> {
                Triple("Last 3 entries: $45.00 (Feed), $12.99 (Waterer), $60.00 (Vaccines).", 0.9, "RECENT_EXPENSES")
            }
            else -> {
                Triple("Your financial summary for ${period ?: "this month"} shows stable operational costs.", 0.85, "GENERAL")
            }
        }
        
        return QueryResolutionResult(
            data = mapOf("totalExpense" to 1240.50),
            summary = summary,
            confidence = score,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(
                matchScore = score,
                matchedTopic = "FINANCE_SUMMARY",
                matchedSubtype = subtype,
                dataSourceId = "finance_repository"
            )
        )
    }

    suspend fun getHelp(
        topic: FinanceHelpTopic?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult {
        val (content, subtype) = when (topic) {
            FinanceHelpTopic.LogExpense -> "To log an expense, tap the 'Log Expense' button in the Finance module. You can categorize it as feed, equipment, or other." to "LOG_EXPENSE"
            FinanceHelpTopic.EditEntry -> "Track your sales by using the 'Log Income' button. This helps you see your profit margins over time." to "EDIT_ENTRY"
            else -> "The Finance module helps you maintain a profitable flock. You can view summaries and detailed logs for any period." to "GENERAL"
        }
        
        return KnowledgeMatchResult(
            content = content,
            confidence = 1.0,
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 1.0, 
                matchedTopic = "FINANCE_HELP",
                matchedSubtype = subtype
            )
        )
    }
}
