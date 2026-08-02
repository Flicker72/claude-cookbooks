package com.holidate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.holidate.app.R
import com.holidate.app.data.db.ProfileEntity
import com.holidate.app.ui.components.InterestChips
import com.holidate.app.ui.components.ProfilePhoto

@Composable
fun DiscoverScreen(
    profiles: List<ProfileEntity>,
    onLike: (String) -> Unit,
    onPass: (String) -> Unit,
) {
    val current = profiles.firstOrNull()
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        if (current == null) {
            EmptyDiscover()
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileCard(profile = current, modifier = Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                ) {
                    FloatingActionButton(
                        onClick = { onPass(current.nodeId) },
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.pass))
                    }
                    FloatingActionButton(
                        onClick = { onLike(current.nodeId) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.like))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: ProfileEntity, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProfilePhoto(
                base64 = profile.photo,
                seed = profile.nodeId,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = buildString {
                        append(profile.displayName)
                        if (profile.age > 0) append(", ${profile.age}")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                }
                if (profile.interests.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    InterestChips(profile.interests)
                }
            }
        }
    }
}

@Composable
private fun EmptyDiscover() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.no_one_around),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_one_around_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
