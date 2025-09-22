// FILE: app/src/main/java/com/example/secureguard/ui/screens/ThreatDetailScreen.kt

package com.example.secureguard.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.secureguard.AppIcon
import com.example.secureguard.data.database.RiskLevel
import com.example.secureguard.data.database.Threat
import com.example.secureguard.ui.theme.HighRiskColor
import com.example.secureguard.ui.theme.LowRiskColor
import com.example.secureguard.ui.theme.MediumRiskColor
import com.example.secureguard.viewmodel.SecurityViewModel

@Composable
fun ThreatDetailScreen(threat: Threat, viewModel: SecurityViewModel, onBack: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Info Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = threat.packageName, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(threat.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(threat.packageName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Threat Details Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val riskColor = when (threat.riskLevel) {
                    RiskLevel.HIGH -> HighRiskColor
                    RiskLevel.MEDIUM -> MediumRiskColor
                    RiskLevel.LOW -> LowRiskColor
                }
                Text("Risk Level: ${threat.riskLevel.name}", fontWeight = FontWeight.Bold, color = riskColor)
                Text("Threat Type: ${threat.threatType}", fontWeight = FontWeight.SemiBold)
                Text("Details:", fontWeight = FontWeight.SemiBold)
                threat.description.split(",").forEach {
                    if (it.isNotBlank()) {
                        Text("• ${it.trim()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.addToWhitelist(threat.packageName); onBack() }, modifier = Modifier.weight(1f)) {
                Text("Whitelist")
            }
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HighRiskColor)
            ) {
                Text("Uninstall")
            }
        }

        // Add a back button for easier navigation
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to List")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Uninstall ${threat.appName}?") },
            text = { Text("This will remove the application from your device. Are you sure?") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = android.net.Uri.parse("package:${threat.packageName}")
                    context.startActivity(intent)
                    showDialog = false
                    onBack()
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}