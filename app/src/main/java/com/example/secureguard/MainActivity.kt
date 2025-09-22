// FILE: app/src/main/java/com/example/secureguard/MainActivity.kt

package com.example.secureguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import com.example.secureguard.data.database.SecureGuardDatabase
import com.example.secureguard.data.database.Threat
import com.example.secureguard.data.repository.SecurityRepository
import com.example.secureguard.ui.screens.HomeScreen
import com.example.secureguard.ui.screens.SettingsScreen
import com.example.secureguard.ui.screens.ThreatDetailScreen
import com.example.secureguard.ui.theme.SecureGuardTheme
import com.example.secureguard.viewmodel.SecurityViewModel
import com.example.secureguard.viewmodel.SecurityViewModelFactory
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SecurityViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.refreshPermissionsStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = SecureGuardDatabase.getDatabase(applicationContext)
        val repository = SecurityRepository(applicationContext, database.threatDao(), database.whitelistDao())
        val viewModelFactory = SecurityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[SecurityViewModel::class.java]

        setContent {
            SecureGuardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SecureGuardApp(viewModel)
                }
            }
        }

        requestPermissions()
        viewModel.startPeriodicScans()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val pm = LocalContext.current.packageManager
    val icon: Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    Image(
        painter = rememberAsyncImagePainter(model = icon),
        contentDescription = "App Icon",
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureGuardApp(viewModel: SecurityViewModel) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureGuard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            SecureGuardBottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavHost(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
fun SecureGuardBottomNavigation(navController: NavController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Dashboard") },
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun AppNavHost(navController: NavHostController, viewModel: SecurityViewModel) {
    val gson = Gson()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel) { threat ->
                val threatJson = gson.toJson(threat)
                val encodedJson = URLEncoder.encode(threatJson, "UTF-8")
                navController.navigate("details/$encodedJson")
            }
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }
        composable("details/{threatJson}") { backStackEntry ->
            val threatJson = backStackEntry.arguments?.getString("threatJson")
            val decodedJson = URLDecoder.decode(threatJson, "UTF-8")
            val threat = gson.fromJson(decodedJson, Threat::class.java)
            ThreatDetailScreen(threat = threat, viewModel = viewModel) {
                navController.popBackStack()
            }
        }
    }
}