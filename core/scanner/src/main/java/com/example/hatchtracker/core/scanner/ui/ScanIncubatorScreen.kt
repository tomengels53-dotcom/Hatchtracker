package com.example.hatchtracker.core.scanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.scanner.api.IncubatorReadingDraft
import com.example.hatchtracker.core.scanner.domain.IncubatorOcrParser
import com.example.hatchtracker.core.scanner.mlkit.TextAnalyzer
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun ScanIncubatorScreen(
    onReadingCaptured: (IncubatorReadingDraft) -> Unit,
    onBack: () -> Unit
) {
    var rawTextLive by remember { mutableStateOf("") }

    val parser = remember { IncubatorOcrParser() }
    val draft = remember(rawTextLive) { parser.parse(rawTextLive) }

    val analyzer = remember {
        TextAnalyzer(onTextDetected = { text ->
            rawTextLive = text
        })
    }
    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }

    ScanScaffold(
        title = stringResource(UiR.string.scan_incubator_screen_title),
        onBack = onBack,
        analyzer = analyzer,
        overlay = {
            ScannerOverlayGuide(
                widthPct = 0.8f,
                heightPct = 0.3f,
                title = stringResource(UiR.string.align_screen_in_box)
            )
        },
        bottomContent = {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(stringResource(UiR.string.live_reading), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(UiR.string.temp_value_c_format, draft.temperatureC ?: "--"))
                Text(stringResource(UiR.string.humidity_value_percent_format, draft.humidityPercent ?: "--"))

                if (draft.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(UiR.string.warnings_list_format, draft.warnings.joinToString()),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onReadingCaptured(draft) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(UiR.string.use_this_reading_action))
                }
            }
        }
    )
}
