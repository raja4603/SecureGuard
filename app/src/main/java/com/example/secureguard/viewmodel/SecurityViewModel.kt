package com.example.secureguard.viewmodel

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.secureguard.data.database.Threat
import com.example.secureguard.data.database.WhitelistedApp
import com.example.secureguard.data.repository.SecurityRepository
import com.example.secureguard.workers.SecurityWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SecurityViewModel(private val repository: SecurityRepository) : ViewModel() {

    // --- CORRECTED STATEFLOW INITIALIZATION ---
    // Convert the 'cold' Flow from the repository into a 'hot' StateFlow for the UI.
    val threats: StateFlow<List<Threat>> = repository.allThreats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val whitelistedApps: StateFlow<List<WhitelistedApp>> = repository.whitelistedApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    // --- END OF CORRECTION ---

    private val _scanProgress = mutableStateOf(0f)
    val scanProgress: State<Float> = _scanProgress

    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _hasUsageStatsPermission = mutableStateOf(false)
    val hasUsageStatsPermission: State<Boolean> = _hasUsageStatsPermission

    // This variable is used to pass the selected threat to the details screen
    var selectedThreat: Threat? = null

    val securityScore: StateFlow<Int> = threats.map { threatList ->
        val score = 100 - (threatList.count { it.riskLevel == com.example.secureguard.data.database.RiskLevel.HIGH } * 15) -
                (threatList.count { it.riskLevel == com.example.secureguard.data.database.RiskLevel.MEDIUM } * 5) -
                (threatList.count { it.riskLevel == com.example.secureguard.data.database.RiskLevel.LOW } * 2)
        score.coerceIn(0, 100)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 100
    )

    val riskLevelLabel: StateFlow<String> = securityScore.map { score ->
        when {
            score >= 90 -> "Device is Secure"
            score >= 60 -> "Low Risk"
            score >= 40 -> "Medium Risk"
            else -> "High Risk"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Device is Secure"
    )

    init {
        refreshPermissionsStatus()
    }

    fun performScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = 0f
            for (i in 1..10) {
                kotlinx.coroutines.delay(150)
                _scanProgress.value = i / 10f
            }
            repository.refreshThreats()
            _isScanning.value = false
        }
    }

    fun startPeriodicScans() {
        val scanWorkRequest = PeriodicWorkRequestBuilder<SecurityWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(repository.context).enqueueUniquePeriodicWork(
            "SecurityScanWork",
            ExistingPeriodicWorkPolicy.KEEP,
            scanWorkRequest
        )
    }

    fun addToWhitelist(packageName: String) {
        viewModelScope.launch { repository.addToWhitelist(packageName) }
    }

    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch { repository.removeFromWhitelist(packageName) }
    }

    fun refreshPermissionsStatus() {
        _hasUsageStatsPermission.value = hasUsageStatsPermission(repository.context)
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

class SecurityViewModelFactory(private val repository: SecurityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

