package com.holidate.app.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.holidate.app.R

/** The runtime permissions Nearby Connections needs, which vary a lot across Android versions. */
private fun requiredPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    // Nearby discovery needs location below Android 13 (and always below Android 12).
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

/**
 * Requests the nearby-device permissions and renders [content] only once they are all granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val permissions = rememberMultiplePermissionsState(requiredPermissions())

    if (permissions.allPermissionsGranted) {
        content()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.permissions_needed),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.permissions_rationale),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { permissions.launchMultiplePermissionRequest() }) {
            Text(stringResource(R.string.grant_permissions))
        }
    }
}
