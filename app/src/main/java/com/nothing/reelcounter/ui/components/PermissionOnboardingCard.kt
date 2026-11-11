package com.nothing.reelcounter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nothing.reelcounter.ui.theme.NothingBlack
import com.nothing.reelcounter.ui.theme.NothingBorder
import com.nothing.reelcounter.ui.theme.NothingGreyLight
import com.nothing.reelcounter.ui.theme.NothingRed
import com.nothing.reelcounter.ui.theme.NothingSurface
import com.nothing.reelcounter.ui.theme.NothingWhite

@Composable
fun PermissionOnboardingCard(
    isAccessibilityEnabled: Boolean,
    isOverlayGranted: Boolean,
    isNotificationGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allReady = isAccessibilityEnabled && isOverlayGranted

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NothingSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (allReady) NothingBorder else NothingRed
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (allReady) NothingWhite else NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PERMISSIONS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NothingWhite
                    )
                }

                Text(
                    text = if (allReady) "READY" else "ACTION REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allReady) NothingGreyLight else NothingRed
                )
            }

            PermissionStepRow(
                title = "ACCESSIBILITY SERVICE",
                isGranted = isAccessibilityEnabled,
                actionLabel = if (isAccessibilityEnabled) "DONE" else "ENABLE",
                onClick = onOpenAccessibilitySettings
            )

            PermissionStepRow(
                title = "DRAW OVERLAY",
                isGranted = isOverlayGranted,
                actionLabel = if (isOverlayGranted) "DONE" else "ALLOW",
                onClick = onOpenOverlaySettings
            )

            if (!isNotificationGranted) {
                PermissionStepRow(
                    title = "NOTIFICATIONS",
                    isGranted = false,
                    actionLabel = "ALLOW",
                    onClick = onRequestNotificationPermission
                )
            }
        }
    }
}

@Composable
private fun PermissionStepRow(
    title: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NothingBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGranted) NothingGreyLight else NothingWhite
        )

        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isGranted) NothingBorder else NothingWhite
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isGranted) NothingBlack else NothingWhite,
                contentColor = if (isGranted) NothingGreyLight else NothingBlack
            )
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) NothingGreyLight else NothingBlack
            )
        }
    }
}
