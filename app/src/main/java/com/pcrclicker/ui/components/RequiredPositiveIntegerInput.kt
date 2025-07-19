package com.pcrclicker.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun RowScope.RequiredPositiveIntegerInput(
    number: String,
    labelText: String,
    supportingText: String,
    suffix: String,
    onChange: (number: String) -> Unit
) {
    val isError = remember { mutableStateOf(false) }
    TextField(
        value = number,
        onValueChange = { newValue ->
            if (newValue.isEmpty()) {
                onChange(number)
                isError.value = true
                return@TextField
            }

            if (newValue.all { it.isDigit() }) {
                try {
                    newValue.toInt()
                    onChange(newValue)
                    isError.value = false
                } catch (_: NumberFormatException) {
                    isError.value = true
                }
            } else {
                isError.value = true
            }
        },
        modifier = Modifier.weight(1f),
        label = { Text(labelText) },
        suffix = if (suffix == "") null else ({ Text(suffix) }),
        supportingText = {
            if (isError.value) {
                Text(
                    text = "请输入0-${Int.MAX_VALUE}的整数",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (supportingText != "") {
                Text(supportingText)
            }
        },
        isError = isError.value,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
    )
}
