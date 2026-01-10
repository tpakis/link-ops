package com.manjee.linkops.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manjee.linkops.domain.model.Device
import com.manjee.linkops.ui.theme.LinkOpsColors

/**
 * Device card component for displaying device information
 *
 * @param device Device information to display
 * @param isSelected Whether this device is currently selected
 * @param onClick Callback when card is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val stateIndicatorColor = when (device.state) {
        Device.DeviceState.ONLINE -> LinkOpsColors.DeviceOnline
        Device.DeviceState.OFFLINE -> LinkOpsColors.DeviceOffline
        Device.DeviceState.UNAUTHORIZED -> LinkOpsColors.DeviceUnauthorized
        Device.DeviceState.UNKNOWN -> LinkOpsColors.Unknown
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = device.state == Device.DeviceState.ONLINE,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // State indicator dot
            StatusDot(
                color = stateIndicatorColor,
                size = 12
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Device info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Model name
                Text(
                    text = device.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Serial number
                Text(
                    text = device.serialNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // OS Version tag
                    if (device.osVersion.isNotEmpty() && device.osVersion != "Unknown") {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Android ${device.osVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // SDK Level tag
                    if (device.sdkLevel > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SDK ${device.sdkLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connection type badge
                    ConnectionTypeBadge(connectionType = device.connectionType)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // State badge
            DeviceStateBadge(state = device.state)
        }
    }
}

/**
 * Compact device card for smaller layouts
 */
@Composable
fun DeviceCardCompact(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val stateIndicatorColor = when (device.state) {
        Device.DeviceState.ONLINE -> LinkOpsColors.DeviceOnline
        Device.DeviceState.OFFLINE -> LinkOpsColors.DeviceOffline
        Device.DeviceState.UNAUTHORIZED -> LinkOpsColors.DeviceUnauthorized
        Device.DeviceState.UNKNOWN -> LinkOpsColors.Unknown
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = device.state == Device.DeviceState.ONLINE,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = stateIndicatorColor)

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.serialNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.model} | Android ${device.osVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
