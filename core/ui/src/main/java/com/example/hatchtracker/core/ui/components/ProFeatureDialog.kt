package com.example.hatchtracker.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun ProFeatureDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onUpgradeClick: () -> Unit,
    title: String = "",
    text: String = ""
) {
    val dialogTitle = if (title.isBlank()) stringResource(com.example.hatchtracker.core.ui.R.string.pro_feature_title) else title
    val dialogText = if (text.isBlank()) stringResource(com.example.hatchtracker.core.ui.R.string.pro_feature_scanning_copy) else text

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                Button(onClick = {
                    onDismissRequest()
                    onUpgradeClick()
                }) {
                    Text(stringResource(com.example.hatchtracker.core.ui.R.string.upgrade_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(com.example.hatchtracker.core.ui.R.string.not_now_action))
                }
            }
        )
    }
}
