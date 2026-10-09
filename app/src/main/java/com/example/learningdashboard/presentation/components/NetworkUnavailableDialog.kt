package com.example.learningdashboard.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.learningdashboard.util.Constants

/**
 * Reusable dialog alerting the user that the device is offline or network is unavailable.
 */
@Composable
fun NetworkUnavailableDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = Constants.Network.NETWORK_UNAVAILABLE_TITLE,
    message: String = Constants.Network.NETWORK_UNAVAILABLE_MESSAGE
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        },
        modifier = modifier
    )
}
