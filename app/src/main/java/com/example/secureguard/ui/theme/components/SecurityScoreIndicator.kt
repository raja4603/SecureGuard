package com.example.secureguard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secureguard.ui.theme.HighRiskColor
import com.example.secureguard.ui.theme.LowRiskColor
import com.example.secureguard.ui.theme.MediumRiskColor

@Composable
fun SecurityScoreIndicator(
    score: Int,
    isScanning: Boolean,
    progress: Float,
    riskLevel: String // <-- ADD THIS NEW PARAMETER
) {
    val animatedScore by animateFloatAsState(targetValue = score / 100f, label = "scoreAnimation")
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnimation")

    val scoreColor = when {
        score >= 90 -> MaterialTheme.colorScheme.primary
        score >= 60 -> LowRiskColor
        score >= 40 -> MediumRiskColor
        else -> HighRiskColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 16.dp,
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        } else {
            CircularProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 16.dp,
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Display the new risk level label
            Text(
                text = riskLevel,
                fontSize = 18.sp,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
