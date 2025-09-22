// FILE: app/src/main/java/com/example/secureguard/ui/screens/HomeScreen.kt

package com.example.secureguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.secureguard.data.database.Threat
import com.example.secureguard.ui.components.SecurityScoreIndicator
import com.example.secureguard.ui.components.ThreatList
import com.example.secureguard.viewmodel.SecurityViewModel

@Composable
fun HomeScreen(viewModel: SecurityViewModel, onThreatClick: (Threat) -> Unit) {
    val threats by viewModel.threats.collectAsState()
    val isScanning by viewModel.isScanning
    val scanProgress by viewModel.scanProgress
    val securityScore by viewModel.securityScore.collectAsState()
    val riskLevelLabel by viewModel.riskLevelLabel.collectAsState()

    // Automatically perform a scan when the screen is first launched
    LaunchedEffect(Unit) {
        viewModel.performScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SecurityScoreIndicator(
            score = securityScore,
            isScanning = isScanning,
            progress = scanProgress,
            riskLevel = riskLevelLabel
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.performScan() },
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (isScanning) "SCANNING..." else "SCAN NOW")
        }

        Spacer(modifier = Modifier.height(24.dp))

        ThreatList(threats = threats, onThreatClick = onThreatClick)
    }
}