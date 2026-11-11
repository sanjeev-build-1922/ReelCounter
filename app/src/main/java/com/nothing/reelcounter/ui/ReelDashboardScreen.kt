package com.nothing.reelcounter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nothing.reelcounter.data.DailyBreakdown
import com.nothing.reelcounter.data.ReelPlatform
import com.nothing.reelcounter.ui.components.DotMatrixNumber
import com.nothing.reelcounter.ui.components.DotMatrixProgressBar
import com.nothing.reelcounter.ui.components.PermissionOnboardingCard
import com.nothing.reelcounter.ui.theme.NothingBlack
import com.nothing.reelcounter.ui.theme.NothingBorder
import com.nothing.reelcounter.ui.theme.NothingDotUnlit
import com.nothing.reelcounter.ui.theme.NothingGreyLight
import com.nothing.reelcounter.ui.theme.NothingGreyMuted
import com.nothing.reelcounter.ui.theme.NothingRed
import com.nothing.reelcounter.ui.theme.NothingSurface
import com.nothing.reelcounter.ui.theme.NothingWhite
import java.util.Locale

@Composable
fun ReelDashboardScreen(
    uiState: ReelDashboardUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleHud: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onAdjustDailyLimit: (Int) -> Unit,
    onPreviewFloatingHud: () -> Unit,
    onSimulateSwipe: (ReelPlatform) -> Unit,
    onResetToday: () -> Unit
) {
    var showPermissionsPanel by remember { mutableStateOf(false) }
    val permissionsComplete = uiState.isAccessibilityEnabled && uiState.isOverlayPermissionGranted

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NothingBlack,
        topBar = {
            NothingTopHeader(
                permissionsComplete = permissionsComplete,
                onTogglePermissionPanel = { showPermissionsPanel = !showPermissionsPanel }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission panel (clean & compact, auto-hides once granted)
            if (!permissionsComplete || showPermissionsPanel) {
                item {
                    PermissionOnboardingCard(
                        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                        isOverlayGranted = uiState.isOverlayPermissionGranted,
                        isNotificationGranted = uiState.isNotificationPermissionGranted,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onRequestNotificationPermission = onRequestNotificationPermission
                    )
                }
            }

            // Hero Daily Dot-Matrix Counter Card (Super clean, no verbose text walls)
            item {
                DailyCounterHeroCard(
                    todayCount = uiState.todayCount,
                    dailyLimit = uiState.dailyLimit,
                    limitProgress = uiState.limitProgress,
                    isOverLimit = uiState.isOverDailyLimit,
                    estimatedMinutes = uiState.estimatedMinutesToday,
                    allTimeTotal = uiState.allTimeTotal
                )
            }

            // Platform Breakdown (Clean 2-column cards)
            item {
                PlatformBreakdownCard(
                    breakdown = uiState.breakdown,
                    onSimulateInstagram = { onSimulateSwipe(ReelPlatform.INSTAGRAM) },
                    onSimulateYoutube = { onSimulateSwipe(ReelPlatform.YOUTUBE) }
                )
            }

            // Settings & Quick Actions
            item {
                HudAndTelemetryControlsCard(
                    hudEnabled = uiState.hudEnabled,
                    hapticsEnabled = uiState.hapticsEnabled,
                    dailyLimit = uiState.dailyLimit,
                    onToggleHud = onToggleHud,
                    onToggleHaptics = onToggleHaptics,
                    onAdjustDailyLimit = onAdjustDailyLimit,
                    onPreviewFloatingHud = onPreviewFloatingHud,
                    onResetToday = onResetToday
                )
            }
        }
    }
}

@Composable
private fun NothingTopHeader(
    permissionsComplete: Boolean,
    onTogglePermissionPanel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "REEL // COUNTER",
            style = MaterialTheme.typography.headlineMedium,
            color = NothingWhite
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(
                    width = 1.dp,
                    color = if (permissionsComplete) NothingBorder else NothingRed,
                    shape = RoundedCornerShape(50)
                )
                .background(NothingSurface)
                .clickable(onClick = onTogglePermissionPanel)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (permissionsComplete) NothingWhite else NothingRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (permissionsComplete) "ONLINE" else "SETUP",
                style = MaterialTheme.typography.labelSmall,
                color = if (permissionsComplete) NothingWhite else NothingRed
            )
        }
    }
}

@Composable
private fun DailyCounterHeroCard(
    todayCount: Int,
    dailyLimit: Int,
    limitProgress: Float,
    isOverLimit: Boolean,
    estimatedMinutes: Int,
    allTimeTotal: Int
) {
    val formattedCount = String.format(Locale.US, "%03d", todayCount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NothingSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverLimit) NothingRed else NothingBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY",
                    style = MaterialTheme.typography.titleMedium,
                    color = NothingWhite
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, NothingBorder, RoundedCornerShape(50))
                        .background(NothingBlack)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formattedCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverLimit) NothingRed else NothingWhite
                    )
                }
            }

            // Dot-matrix display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .padding(vertical = 22.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                DotMatrixNumber(
                    value = todayCount,
                    minDigits = 3,
                    dotSize = 7.dp,
                    dotSpacing = 3.5.dp,
                    digitSpacing = 18.dp,
                    activeColor = if (isOverLimit) NothingRed else NothingWhite,
                    inactiveColor = NothingDotUnlit
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverLimit) "LIMIT EXCEEDED" else "DAILY PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverLimit) NothingRed else NothingGreyMuted
                    )
                    Text(
                        text = "$todayCount / $dailyLimit",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingWhite
                    )
                }

                DotMatrixProgressBar(
                    progress = limitProgress,
                    totalColumns = 28,
                    activeColor = if (isOverLimit) NothingRed else NothingWhite,
                    inactiveColor = NothingDotUnlit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(color = NothingBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricColumn(
                    label = "TIME",
                    value = "${estimatedMinutes}m"
                )
                TelemetryMetricColumn(
                    label = "LOCK",
                    value = "1.3s"
                )
                TelemetryMetricColumn(
                    label = "TOTAL",
                    value = String.format(Locale.US, "%04d", allTimeTotal)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricColumn(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NothingGreyMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = NothingWhite
        )
    }
}

@Composable
private fun PlatformBreakdownCard(
    breakdown: DailyBreakdown,
    onSimulateInstagram: () -> Unit,
    onSimulateYoutube: () -> Unit
) {
    val igPct = (breakdown.instagramRatio * 100).toInt()
    val ytPct = (breakdown.youtubeRatio * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NothingSurface),
        border = BorderStroke(1.dp, NothingBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLATFORMS",
                    style = MaterialTheme.typography.titleMedium,
                    color = NothingWhite
                )
                Text(
                    text = "IG $igPct%  •  YT $ytPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingGreyLight
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlatformStatBox(
                    name = "INSTAGRAM",
                    count = breakdown.instagramCount,
                    accentColor = NothingWhite,
                    onSimulateTap = onSimulateInstagram,
                    modifier = Modifier.weight(1f)
                )

                PlatformStatBox(
                    name = "YOUTUBE",
                    count = breakdown.youtubeCount,
                    accentColor = NothingRed,
                    onSimulateTap = onSimulateYoutube,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlatformStatBox(
    name: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    onSimulateTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )

        DotMatrixNumber(
            value = count,
            minDigits = 3,
            dotSize = 4.dp,
            dotSpacing = 2.dp,
            digitSpacing = 8.dp,
            activeColor = accentColor,
            inactiveColor = NothingDotUnlit
        )

        OutlinedButton(
            onClick = onSimulateTap,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, NothingBorder),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text(
                text = "+1 TEST",
                style = MaterialTheme.typography.labelSmall,
                color = NothingGreyLight
            )
        }
    }
}

@Composable
private fun HudAndTelemetryControlsCard(
    hudEnabled: Boolean,
    hapticsEnabled: Boolean,
    dailyLimit: Int,
    onToggleHud: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onAdjustDailyLimit: (Int) -> Unit,
    onPreviewFloatingHud: () -> Unit,
    onResetToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NothingSurface),
        border = BorderStroke(1.dp, NothingBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CONTROLS",
                style = MaterialTheme.typography.titleMedium,
                color = NothingWhite
            )

            SettingToggleRow(
                title = "FLOATING OVERLAY",
                checked = hudEnabled,
                onCheckedChange = onToggleHud
            )

            SettingToggleRow(
                title = "HAPTIC FEEDBACK",
                checked = hapticsEnabled,
                onCheckedChange = onToggleHaptics
            )

            HorizontalDivider(color = NothingBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY LIMIT",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NothingWhite
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallStepButton(label = "-10", onClick = { onAdjustDailyLimit(-10) })
                    Text(
                        text = String.format(Locale.US, "%03d", dailyLimit),
                        style = MaterialTheme.typography.titleMedium,
                        color = NothingWhite
                    )
                    SmallStepButton(label = "+10", onClick = { onAdjustDailyLimit(10) })
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewFloatingHud,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NothingWhite),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NothingBlack,
                        contentColor = NothingWhite
                    )
                ) {
                    Text(
                        text = "TEST OVERLAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingWhite
                    )
                }

                OutlinedButton(
                    onClick = onResetToday,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NothingRed),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NothingBlack,
                        contentColor = NothingRed
                    )
                ) {
                    Text(
                        text = "RESET [000]",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = NothingWhite
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NothingBlack,
                checkedTrackColor = NothingWhite,
                uncheckedThumbColor = NothingGreyMuted,
                uncheckedTrackColor = NothingBlack,
                uncheckedBorderColor = NothingBorder
            )
        )
    }
}

@Composable
private fun SmallStepButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
            .background(NothingBlack)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NothingWhite,
            fontWeight = FontWeight.Bold
        )
    }
}
