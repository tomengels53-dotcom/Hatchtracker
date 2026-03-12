package com.example.hatchtracker.feature.auth

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    type: LegalDocumentType,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    val title = when (type) {
        LegalDocumentType.PRIVACY_POLICY -> "Privacy Policy"
        LegalDocumentType.TERMS_OF_SERVICE -> "Terms of Service"
    }

    LaunchedEffect(type) {
        content = loadAssetContent(context, type.fileName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

enum class LegalDocumentType(val fileName: String) {
    PRIVACY_POLICY("legal/PRIVACY_POLICY.md"),
    TERMS_OF_SERVICE("legal/TERMS_OF_SERVICE.md")
}

private fun loadAssetContent(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        String(buffer)
    } catch (e: Exception) {
        "Error loading $fileName: ${e.localizedMessage}"
    }
}
