package com.example.hatchtracker.feature.breeding.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.model.breeding.GeneticInsightUiModel
import com.example.hatchtracker.model.genetics.InsightConfidence

@Composable
fun BreedingIntelligenceCard(
    uiModel: GeneticInsightUiModel?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.breeding_intelligence_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (uiModel?.isLoading == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ConfidenceBadge(uiModel?.confidence ?: InsightConfidence.MODERATE)
                }
            }

            AnimatedVisibility(
                visible = uiModel != null && !uiModel.isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (uiModel != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiModel.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MetricIndicator("Stability", uiModel.stabilityLabel, uiModel.stabilityScore / 100f)
                            MetricIndicator("Diversity", uiModel.diversityLabel, 1f)
                        }

                        val topWarning = uiModel.topWarning
                        if (!topWarning.isNullOrBlank()) {
                            WarningBanner(topWarning)
                        }

                        if (uiModel.primaryActions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.breeding_intelligence_action_guidance),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            uiModel.primaryActions.take(2).forEach { action ->
                                ActionItem(action.title.toString(), action.suggestion.toString())
                            }
                        }
                    }
                }
            }

            if (uiModel == null) {
                Text(
                    text = stringResource(R.string.breeding_intelligence_empty_state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MetricIndicator(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surface,
            color = if (progress > 0.7f) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    }
}

@Composable
private fun ConfidenceBadge(confidence: InsightConfidence) {
    val (color, text) = when (confidence) {
        InsightConfidence.HIGH -> Color(0xFF4CAF50) to "High Confidence"
        InsightConfidence.MODERATE -> Color(0xFFFF9800) to "Moderate Confidence"
        InsightConfidence.LOW -> Color(0xFFF44336) to "Low Confidence"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WarningBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "!",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionItem(title: String, suggestion: String) {
    Column {
        Text(
            text = stringResource(R.string.breeding_intelligence_action_item_format, title),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
