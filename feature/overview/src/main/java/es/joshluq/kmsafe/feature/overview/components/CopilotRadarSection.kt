package es.joshluq.kmsafe.feature.overview.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.buttons.CanvasKitButtonVariant
import es.joshluq.canvaskit.components.cards.CanvasKitCard
import es.joshluq.canvaskit.components.cards.CanvasKitCardVariant
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.core.ui.util.safeClick
import es.joshluq.kmsafe.core.ui.util.safeClickable
import es.joshluq.kmsafe.feature.overview.R

/**
 * Layer 2 Decision Radar & Copilot Control Section.
 * Surfaces actionable daily metrics and trip recording controls adapted to the subscription tier.
 */
@Composable
fun CopilotRadarSection(
    modifier: Modifier = Modifier,
    dailyQuotaKm: Double,
    isPremium: Boolean,
    isBluetoothConnected: Boolean,
    hasPermissions: Boolean = true,
    autoTrackingEnabled: Boolean = true,
    onStartTripClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onRequestPermissions: () -> Unit = {},
    onNavigateToPreferences: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("copilot_radar_section"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Today's Safe Quota
        CanvasKitCard(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 130.dp),
            variant = CanvasKitCardVariant.Outlined,
            header = {
                Text(
                    text = stringResource(R.string.overview_radar_daily_quota_title),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${dailyQuotaKm.toInt()} km",
                    style = CanvasKitTheme.typography.headingLarge,
                    fontWeight = FontWeight.Black,
                    color = CanvasKitTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.overview_radar_daily_quota_subtitle),
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary
                )
            }
        }

        // Card 2: Copilot Trip Control (Free vs Premium)
        val cardModifier = if (isPremium) {
            when {
                !autoTrackingEnabled -> Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 130.dp)
                    .safeClickable { onNavigateToPreferences() }
                    .testTag("copilot_radar_disabled")
                !hasPermissions -> Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 130.dp)
                    .safeClickable { onRequestPermissions() }
                    .testTag("copilot_radar_no_permissions")
                else -> Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 130.dp)
            }
        } else {
            Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 130.dp)
        }

        CanvasKitCard(
            modifier = cardModifier,
            variant = CanvasKitCardVariant.Outlined,
            header = {
                Text(
                    text = if (isPremium) {
                        stringResource(R.string.overview_radar_smartcopilot_title)
                    } else {
                        stringResource(R.string.overview_radar_assisted_copilot_title)
                    },
                    style = CanvasKitTheme.typography.labelSmall,
                    color = CanvasKitTheme.colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        ) {
            if (isPremium) {
                if (!autoTrackingEnabled) {
                    // State 1: Auto-Tracking Disabled in Preferences
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CanvasKitTheme.colors.textSecondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = CanvasKitTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.overview_radar_autotracking_disabled),
                                style = CanvasKitTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.overview_radar_enable_in_settings),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.brandAccent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else if (!hasPermissions) {
                    // State 2: Missing Permissions (Warning + Invite to configure)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CanvasKitTheme.colors.warning.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CanvasKitTheme.colors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.overview_radar_permissions_required),
                                style = CanvasKitTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.warning,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.overview_radar_grant_permissions_desc),
                                style = CanvasKitTheme.typography.labelSmall,
                                color = CanvasKitTheme.colors.brandAccent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CanvasKitTheme.colors.brandAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    // State 3 & 4: Premium Auto-Tracking (Connected or Standby)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CanvasKitTheme.colors.brandAccent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBluetoothConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = CanvasKitTheme.colors.brandAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = if (isBluetoothConnected) {
                                    stringResource(R.string.overview_radar_car_connected)
                                } else {
                                    stringResource(R.string.overview_radar_standby)
                                },
                                style = CanvasKitTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CanvasKitTheme.colors.brandAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isBluetoothConnected) {
                                stringResource(R.string.overview_radar_record_on_start)
                            } else {
                                stringResource(R.string.overview_radar_auto_detection_active)
                            },
                            style = CanvasKitTheme.typography.labelSmall,
                            color = CanvasKitTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Free 1-Tap Manual Start
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CanvasKitButton(
                        text = stringResource(R.string.overview_radar_start_trip_button),
                        icon = Icons.Default.PlayArrow,
                        variant = CanvasKitButtonVariant.Primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("free_start_trip_btn"),
                        onClick = safeClick { onStartTripClick() }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .safeClickable { onUpgradeClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CanvasKitTheme.colors.brandAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.overview_radar_auto_with_pro),
                            style = CanvasKitTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CanvasKitTheme.colors.brandAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "CopilotRadar - Free Tier")
@Preview(name = "CopilotRadar - Free Tier Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCopilotRadarFree() {
    CanvasKitTheme {
        CopilotRadarSection(
            dailyQuotaKm = 68.0,
            isPremium = false,
            isBluetoothConnected = false,
            onStartTripClick = {},
            onUpgradeClick = {}
        )
    }
}

@Preview(name = "CopilotRadar - Premium Connected")
@Composable
fun PreviewCopilotRadarPremium() {
    CanvasKitTheme {
        CopilotRadarSection(
            dailyQuotaKm = 82.0,
            isPremium = true,
            isBluetoothConnected = true,
            hasPermissions = true,
            onStartTripClick = {},
            onUpgradeClick = {}
        )
    }
}

@Preview(name = "CopilotRadar - Premium No Permissions")
@Composable
fun PreviewCopilotRadarNoPermissions() {
    CanvasKitTheme {
        CopilotRadarSection(
            dailyQuotaKm = 82.0,
            isPremium = true,
            isBluetoothConnected = false,
            hasPermissions = false,
            autoTrackingEnabled = true,
            onStartTripClick = {},
            onUpgradeClick = {},
            onRequestPermissions = {}
        )
    }
}

@Preview(name = "CopilotRadar - Premium Disabled")
@Composable
fun PreviewCopilotRadarDisabled() {
    CanvasKitTheme {
        CopilotRadarSection(
            dailyQuotaKm = 82.0,
            isPremium = true,
            isBluetoothConnected = false,
            hasPermissions = true,
            autoTrackingEnabled = false,
            onStartTripClick = {},
            onUpgradeClick = {},
            onNavigateToPreferences = {}
        )
    }
}
