// FILE: app/src/main/java/com/example/secureguard/ui/screens/SettingsScreen.kt

package com.example.secureguard.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.secureguard.viewmodel.SecurityViewModel

@Composable
fun SettingsScreen(viewModel: SecurityViewModel) {
    val whitelistedApps by viewModel.whitelistedApps.collectAsState(initial = emptyList())
    val hasPermission by viewModel.hasUsageStatsPermission

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionsStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Usage Access", fontWeight = FontWeight.Bold)
                    Text(
                        if (hasPermission) "Permission granted" else "Required for advanced monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!hasPermission) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) {
                        Text("Grant")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Whitelisted Apps", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (whitelistedApps.isEmpty()) {
            Text("No apps have been whitelisted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card {
                LazyColumn {
                    items(whitelistedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.packageName, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.removeFromWhitelist(app.packageName) }) {
                                Text("Remove")
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}