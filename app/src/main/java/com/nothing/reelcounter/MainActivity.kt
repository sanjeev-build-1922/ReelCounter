package com.nothing.reelcounter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.nothing.reelcounter.service.FloatingHudService
import com.nothing.reelcounter.service.ReelAccessibilityService
import com.nothing.reelcounter.ui.ReelDashboardScreen
import com.nothing.reelcounter.ui.ReelViewModel
import com.nothing.reelcounter.ui.theme.ReelCounterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReelViewModel by viewModels {
        ReelViewModel.Factory((application as ReelCounterApp).repository)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        syncPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncPermissionState()

        setContent {
            ReelCounterTheme {
                val uiState by viewModel.uiState.collectAsState()

                ReelDashboardScreen(
                    uiState = uiState,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onToggleHud = { enabled ->
                        viewModel.setHudEnabled(enabled)
                        if (!enabled) {
                            FloatingHudService.stopHud(this)
                        }
                    },
                    onToggleHaptics = viewModel::setHapticsEnabled,
                    onAdjustDailyLimit = viewModel::adjustDailyLimit,
                    onPreviewFloatingHud = ::previewFloatingHud,
                    onSimulateSwipe = viewModel::simulateSwipe,
                    onResetToday = viewModel::resetToday
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncPermissionState()
    }

    private fun syncPermissionState() {
        val accessibilityEnabled = ReelAccessibilityService.isServiceEnabled(this)
        val overlayGranted = Settings.canDrawOverlays(this)
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        viewModel.refreshPermissions(
            isAccessibilityEnabled = accessibilityEnabled,
            isOverlayGranted = overlayGranted,
            isNotificationGranted = notificationGranted
        )
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun previewFloatingHud() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Grant Overlay Permission first to preview the floating HUD pill.",
                Toast.LENGTH_SHORT
            ).show()
            openOverlaySettings()
            return
        }
        viewModel.setHudEnabled(true)
        FloatingHudService.showHud(this)
    }
}
