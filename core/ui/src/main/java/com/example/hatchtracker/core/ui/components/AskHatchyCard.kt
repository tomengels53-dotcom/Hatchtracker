package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R

@Composable
fun AskHatchyCard(
    summary: String? = null,
    title: String = "",
    body: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.hatchy_1),
                contentDescription = "Hatchy",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val resolvedTitle = if (title.isNotBlank()) title else "Hatchy Insight"
                val resolvedBody = when {
                    body.isNotBlank() -> body
                    !summary.isNullOrBlank() -> summary
                    else -> ""
                }
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                if (resolvedBody.isNotBlank()) {
                    Text(
                        text = resolvedBody,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
