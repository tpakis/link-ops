package com.manjee.linkops.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjee.linkops.domain.model.Device
import com.manjee.linkops.domain.model.VerificationState
import com.manjee.linkops.ui.theme.LinkOpsColors

/**
 * Status indicator dot
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 10
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}

/**
 * Device state badge with color indicator
 */
@Composable
fun DeviceStateBadge(
    state: Device.DeviceState,
    modifier: Modifier = Modifier
) {
    val (text, backgroundColor, textColor) = when (state) {
        Device.DeviceState.ONLINE -> Triple(
            "Online",
            LinkOpsColors.SuccessLight,
            LinkOpsColors.Success
        )
        Device.DeviceState.OFFLINE -> Triple(
            "Offline",
            Color(0xFFEEEEEE),
            LinkOpsColors.DeviceOffline
        )
        Device.DeviceState.UNAUTHORIZED -> Triple(
            "Unauthorized",
            LinkOpsColors.WarningLight,
            LinkOpsColors.Warning
        )
        Device.DeviceState.UNKNOWN -> Triple(
            "Unknown",
            Color(0xFFEEEEEE),
            LinkOpsColors.Unknown
        )
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Verification state badge with color indicator
 */
@Composable
fun VerificationBadge(
    state: VerificationState,
    modifier: Modifier = Modifier
) {
    val (text, icon, backgroundColor, textColor) = when (state) {
        VerificationState.VERIFIED -> Quadruple(
            "Verified",
            "✓",
            LinkOpsColors.SuccessLight,
            LinkOpsColors.Verified
        )
        VerificationState.APPROVED -> Quadruple(
            "Approved",
            "✓",
            LinkOpsColors.SuccessLight,
            LinkOpsColors.Verified
        )
        VerificationState.DENIED -> Quadruple(
            "Denied",
            "✗",
            LinkOpsColors.ErrorLight,
            LinkOpsColors.Denied
        )
        VerificationState.UNVERIFIED -> Quadruple(
            "Unverified",
            "?",
            LinkOpsColors.WarningLight,
            LinkOpsColors.Unverified
        )
        VerificationState.LEGACY_FAILURE -> Quadruple(
            "Failed",
            "!",
            LinkOpsColors.ErrorLight,
            LinkOpsColors.Error
        )
        VerificationState.UNKNOWN -> Quadruple(
            "Unknown",
            "•",
            Color(0xFFEEEEEE),
            LinkOpsColors.Unknown
        )
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$icon $text",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Connection type badge (USB, WiFi, Emulator)
 */
@Composable
fun ConnectionTypeBadge(
    connectionType: Device.ConnectionType,
    modifier: Modifier = Modifier
) {
    val (text, icon) = when (connectionType) {
        Device.ConnectionType.USB -> Pair("USB", "🔌")
        Device.ConnectionType.WIFI -> Pair("WiFi", "📶")
        Device.ConnectionType.EMULATOR -> Pair("Emulator", "💻")
    }

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$icon $text",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Helper data class for quadruple values
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
