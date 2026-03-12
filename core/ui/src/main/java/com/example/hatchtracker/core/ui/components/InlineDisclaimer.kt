package com.example.hatchtracker.core.ui.components

import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun InlineDisclaimer(
    text: String = "",
    title: String = "",
    description: String = "",
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        val body = if (text.isNotBlank()) text else listOf(title, description).filter { it.isNotBlank() }.joinToString("\n")
        Text(text = body)
    }
}
