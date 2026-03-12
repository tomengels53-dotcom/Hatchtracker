package com.example.hatchtracker.core.ui.components

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.hatchtracker.common.localization.SupportedLanguages
import java.util.*

@Composable
fun TranslationDebugDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember { calculateTranslationStats(context) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Translation Audit",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stats) { stat ->
                        TranslationStatRow(stat)
                    }
                }
            }
        }
    }
}

@Composable
fun TranslationStatRow(stat: TranslationStat) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat.languageName, fontWeight = FontWeight.Medium)
                Text("${stat.percentage}%", color = if (stat.percentage < 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stat.percentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (stat.percentage < 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                "${stat.translatedCount} / ${stat.totalCount} strings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class TranslationStat(
    val languageName: String,
    val tag: String,
    val translatedCount: Int,
    val totalCount: Int,
    val percentage: Int
)

private fun calculateTranslationStats(context: Context): List<TranslationStat> {
    val stats = mutableListOf<TranslationStat>()
    val baseResources = context.resources
    
    // Use reflection to get all string IDs in the app's R file
    // Note: In real production, we might want a more stable way, but for debug tool it works.
    val stringIds = try {
        val rString = Class.forName("${context.packageName}.R\$string")
        rString.fields.map { it.getInt(null) }
    } catch (e: Exception) {
        listOf<Int>()
    }

    if (stringIds.isEmpty()) return stats

    SupportedLanguages.filter { it.tag != null }.forEach { lang ->
        val tag = lang.tag!!
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(baseResources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        val localizedResources = localizedContext.resources

        var translatedCount = 0
        stringIds.forEach { id ->
            try {
                // If the localized string is different from the base string, it "might" be translated.
                // This is a heuristic because some strings are identical across languages.
                // A better way is to check if the resource is falling back to default.
                // Android doesn't expose "is this from default" easily at runtime without internal APIs.
                // However, we can compare values for now.
                val baseValue = baseResources.getString(id)
                val localizedValue = localizedResources.getString(id)
                
                // Heuristic: If it's different, it's definitely translated.
                // If it's the same, and the language isn't English (base), it's likely NOT translated.
                if (localizedValue != baseValue || tag == "en") {
                    translatedCount++
                }
            } catch (e: Resources.NotFoundException) {
                // Ignore
            }
        }

        stats.add(TranslationStat(
            languageName = context.getString(lang.labelRes),
            tag = tag,
            translatedCount = translatedCount,
            totalCount = stringIds.size,
            percentage = if (stringIds.isNotEmpty()) (translatedCount * 100) / stringIds.size else 0
        ))
    }

    return stats.sortedByDescending { it.percentage }
}
