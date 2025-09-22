package com.example.secureguard.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.secureguard.MainActivity
import com.example.secureguard.R
import com.example.secureguard.ai.AiThreatDetector
import com.example.secureguard.data.database.RiskLevel
import com.example.secureguard.data.database.Threat
import com.example.secureguard.data.database.ThreatDao
import com.example.secureguard.data.database.WhitelistDao
import com.example.secureguard.data.database.WhitelistedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SecurityRepository(
    val context: Context,
    private val threatDao: ThreatDao,
    private val whitelistDao: WhitelistDao
) {
    companion object {
        val DANGEROUS_PERMISSIONS = listOf(
            "android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS",
            "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG", "android.permission.ACCESS_FINE_LOCATION", "android.permission.GET_ACCOUNTS",
            "android.permission.READ_CONTACTS"
        )
        val SUSPICIOUS_INTENTS = listOf(
            "android.intent.action.BOOT_COMPLETED",
            "android.provider.Telephony.SMS_RECEIVED"
        )
        val ALL_FEATURES = DANGEROUS_PERMISSIONS + SUSPICIOUS_INTENTS
    }

    val allThreats: Flow<List<Threat>> = threatDao.getAllThreats()
    val whitelistedApps: Flow<List<WhitelistedApp>> = whitelistDao.getWhitelistedApps()

    suspend fun refreshThreats() {
        withContext(Dispatchers.IO) {
            val whitelist = whitelistDao.getWhitelistedApps().first().map { it.packageName }.toSet()
            val newThreats = scanDevice(whitelist)
            threatDao.deleteAll()
            threatDao.insertAll(newThreats)

            val highRiskThreats = newThreats.filter { it.riskLevel == RiskLevel.HIGH }
            if (highRiskThreats.isNotEmpty()) {
                sendThreatNotification(highRiskThreats.first())
            }
        }
    }

    suspend fun addToWhitelist(packageName: String) {
        whitelistDao.addToWhitelist(WhitelistedApp(packageName))
        refreshThreats()
    }

    suspend fun removeFromWhitelist(packageName: String) {
        whitelistDao.removeFromWhitelist(WhitelistedApp(packageName))
        refreshThreats()
    }

    private fun scanDevice(whitelist: Set<String>): List<Threat> {
        val threats = mutableListOf<Threat>()
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val aiDetector = AiThreatDetector(context)

        if (isDeviceRooted()) {
            threats.add(Threat(appName = "System", packageName = "android", threatType = "Root Access", riskLevel = RiskLevel.HIGH, description = "Device is rooted, which compromises security."))
        }

        for (app in apps) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0 && app.packageName !in whitelist) {
                val appName = pm.getApplicationLabel(app).toString()

                val featureVector = extractFeaturesForApp(app.packageName)
                val riskScore = aiDetector.getRiskScore(featureVector)

                if (riskScore > 0.95) { // Stricter AI threshold
                    threats.add(
                        Threat(
                            appName = appName,
                            packageName = app.packageName,
                            threatType = "AI Analysis",
                            riskLevel = RiskLevel.HIGH,
                            description = "Predicted as high-risk by AI model (Risk Score: ${String.format("%.2f", riskScore)})."
                        )
                    )
                    // If AI flags it as HIGH risk, we don't need to check for other permissions.
                    continue
                }

                // If AI analysis is not HIGH risk, check for dangerous permissions.
                val dangerousPermissionsFound = DANGEROUS_PERMISSIONS.filter {
                    pm.checkPermission(it, app.packageName) == PackageManager.PERMISSION_GRANTED
                }

                if (dangerousPermissionsFound.isNotEmpty()) {
                    // *** NEW TIERED LOGIC ***
                    val riskLevel = when {
                        dangerousPermissionsFound.size >= 2 -> RiskLevel.MEDIUM // 2 or more permissions is Medium Risk
                        else -> RiskLevel.LOW // 1 permission is Low Risk
                    }

                    val description = "Has ${dangerousPermissionsFound.size} potentially risky permission(s): \n" +
                            dangerousPermissionsFound.joinToString(separator = "\n") { "- " + it.substringAfterLast('.') }

                    threats.add(
                        Threat(
                            appName = appName,
                            packageName = app.packageName,
                            threatType = "Permission",
                            riskLevel = riskLevel,
                            description = description
                        )
                    )
                }
            }
        }

        aiDetector.close()
        // We use a different distinctBy logic to allow one AI threat and one Permission threat per app if necessary,
        // but the logic above prevents this, ensuring only the highest risk is recorded.
        return threats.distinctBy { it.packageName }
    }


    private fun extractFeaturesForApp(packageName: String): FloatArray {
        val featureVector = FloatArray(ALL_FEATURES.size) { 0.0f }
        val pm = context.packageManager

        try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS)

            packageInfo.requestedPermissions?.forEach { permission ->
                val index = DANGEROUS_PERMISSIONS.indexOf(permission)
                if (index != -1) {
                    featureVector[index] = 1.0f
                }
            }

            packageInfo.receivers?.forEach { receiverInfo ->
                val index = SUSPICIOUS_INTENTS.indexOfFirst { receiverInfo.name.contains(it, ignoreCase = true) }
                if (index != -1) {
                    featureVector[DANGEROUS_PERMISSIONS.size + index] = 1.0f
                }
            }
        } catch (e: Exception) {
            Log.e("FeatureExtractor", "Could not extract features for $packageName")
        }

        return featureVector
    }


    private fun isDeviceRooted(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun sendThreatNotification(threat: Threat) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "secureguard_threat_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Critical Threats", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_security_notification) // Ensure this drawable exists
            .setContentTitle("🚨 Critical Security Alert!")
            .setContentText("High-risk threat detected: ${threat.appName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(threat.description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}

