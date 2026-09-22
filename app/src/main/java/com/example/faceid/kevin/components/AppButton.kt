package com.example.faceid.kevin.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    modifier: Modifier = Modifier
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier
        ) {
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier
        ) {
            Text(text = text)
        }
    }
}
