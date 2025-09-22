package com.example.secureguard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secureguard.data.database.RiskLevel
import com.example.secureguard.data.database.Threat

@Composable
fun ThreatList(threats: List<Threat>, onThreatClick: (Threat) -> Unit) {
    if (threats.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
            Text("✅", fontSize = 48.sp)
            Text("No Threats Found", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
            Text("Your device appears to be secure.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val groupedThreats = threats.groupBy { it.riskLevel }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groupedThreats.forEach { (riskLevel, threatList) ->
            item {
                ExpandableThreatSection(
                    riskLevel = riskLevel,
                    threats = threatList,
                    onThreatClick = onThreatClick
                )
            }
        }
    }
}

@Composable
fun ExpandableThreatSection(
    riskLevel: RiskLevel,
    threats: List<Threat>,
    onThreatClick: (Threat) -> Unit
) {
    var expanded by remember { mutableStateOf(riskLevel == RiskLevel.HIGH) }
    val (color, icon) = when (riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error to "🚨"
        RiskLevel.MEDIUM -> Color(0xFFFFC107) to "⚠️"
        RiskLevel.LOW -> MaterialTheme.colorScheme.primary to "ℹ️"
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${riskLevel.name} RISK (${threats.size})",
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = "Expand/Collapse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                threats.forEach { threat ->
                    ThreatItem(threat = threat, onThreatClick = onThreatClick)
                }
            }
        }
    }
}

@Composable
fun ThreatItem(threat: Threat, onThreatClick: (Threat) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            .clickable { onThreatClick(threat) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(threat.appName, fontWeight = FontWeight.SemiBold)
                Text(threat.threatType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

