package com.holidate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holidate.app.R
import com.holidate.app.data.db.ProfileEntity
import com.holidate.app.ui.components.InterestChips
import com.holidate.app.ui.components.ProfilePhoto

@Composable
fun ProfileScreen(
    self: ProfileEntity?,
    meshRunning: Boolean,
    nearbyPeerCount: Int,
    onToggleMesh: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfilePhoto(
            base64 = self?.photo,
            seed = self?.nodeId ?: "me",
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = buildString {
                append(self?.displayName ?: "You")
                if ((self?.age ?: 0) > 0) append(", ${self?.age}")
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        self?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(Modifier.height(8.dp))
            Text(bio, style = MaterialTheme.typography.bodyMedium)
        }
        self?.interests?.takeIf { it.isNotEmpty() }?.let { interests ->
            Spacer(Modifier.height(12.dp))
            InterestChips(interests)
        }

        Spacer(Modifier.height(28.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(if (meshRunning) R.string.mesh_on else R.string.mesh_off),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.peers_nearby, nearbyPeerCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = meshRunning, onCheckedChange = onToggleMesh)
            }
        }
    }
}
