package com.example.hatchtracker.core.scanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.scanner.api.ReceiptDraft
import com.example.hatchtracker.core.scanner.domain.ReceiptParser
import com.example.hatchtracker.core.scanner.mlkit.TextAnalyzer
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun ScanReceiptScreen(
    onReceiptCaptured: (ReceiptDraft) -> Unit,
    onBack: () -> Unit
) {
    var rawTextLive by remember { mutableStateOf("") }
    
    val parser = remember { ReceiptParser() }
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
        title = stringResource(UiR.string.scan_receipt_title),
        onBack = onBack,
        analyzer = analyzer,
        overlay = {
            ScannerOverlayGuide(widthPct = 0.8f, heightPct = 0.7f, title = stringResource(UiR.string.align_receipt_in_box)) // receipt is taller
        },
        bottomContent = {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(stringResource(UiR.string.live_preview), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(UiR.string.vendor_value_format, draft.vendor ?: "--"))
                Text(stringResource(UiR.string.total_value_with_currency_format, draft.total ?: "--", draft.currency.orEmpty()))
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onReceiptCaptured(draft) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(UiR.string.use_this_receipt_action))
                }
            }
        }
    )
}
