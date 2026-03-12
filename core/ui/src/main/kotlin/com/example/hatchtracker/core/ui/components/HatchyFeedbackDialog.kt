package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HatchyFeedbackDialog(
    title: String = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.feedback_title_default),
    description: String = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.feedback_description_default),
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.your_feedback)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(comment) },
                enabled = comment.isNotBlank()
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.cancel))
            }
        }
    )
}
