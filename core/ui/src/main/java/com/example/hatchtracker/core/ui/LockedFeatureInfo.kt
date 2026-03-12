package com.example.hatchtracker.core.ui

import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.ui.R

data class LockedFeatureInfo(
    val featureKey: FeatureKey,
    val nameRes: Int,
    val explanationRes: Int,
    val proBenefitRes: Int,
    val iconRes: Int? = null,
    val titleRes: Int = R.string.locked_feature_title
)

object LockedFeatureProvider {
    fun getLockedFeatureInfo(feature: FeatureKey): LockedFeatureInfo? {
        return when (feature) {
            FeatureKey.BREEDING -> LockedFeatureInfo(
                featureKey = feature,
                nameRes = R.string.locked_feature_breeding_name,
                explanationRes = R.string.locked_feature_breeding_explanation,
                proBenefitRes = R.string.locked_feature_breeding_benefit
            )
            FeatureKey.FINANCE -> LockedFeatureInfo(
                featureKey = feature,
                nameRes = R.string.locked_feature_finance_name,
                explanationRes = R.string.locked_feature_finance_explanation,
                proBenefitRes = R.string.locked_feature_finance_benefit,
                titleRes = R.string.locked_feature_title_expert_pro
            )
            // Note: Community might not be in FeatureKey enum yet but we'll stick to what was in the original policy
            else -> null
        }
    }
}
