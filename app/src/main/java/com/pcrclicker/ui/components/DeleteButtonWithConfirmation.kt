package com.pcrclicker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DeleteButtonWithConfirmation(
    title: String,
    message: String,
    onConfirm: () -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog.value = true }
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除",
            tint = MaterialTheme.colorScheme.error
        )
    }
    if (showDialog.value) {
        ConfirmationDialog(
            title = title,
            message = message,
            onConfirm = {
                showDialog.value = false
                onConfirm()
            },
            onDismiss = { showDialog.value = false }
        )
    }
}
