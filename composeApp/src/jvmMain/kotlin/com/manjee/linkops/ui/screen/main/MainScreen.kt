package com.manjee.linkops.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjee.linkops.domain.model.IntentConfig
import com.manjee.linkops.ui.component.*
import com.manjee.linkops.ui.theme.LinkOpsColors

/**
 * Main Screen - Dashboard view for LinkOps
 *
 * Layout:
 * - Left panel: Controls (ADB status, devices, app links, intent firing)
 * - Right panel: Log output
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val logText by viewModel.logText.collectAsState()

    var showIntentDialog by remember { mutableStateOf(false) }
    var intentUri by remember { mutableStateOf("") }

    val isLoading = uiState.isLoadingDevices || uiState.isLoadingAppLinks || uiState.isFiringIntent

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left Panel - Controls
            ControlPanel(
                uiState = uiState,
                intentUri = intentUri,
                onIntentUriChange = { intentUri = it },
                onCheckAdb = { viewModel.checkAdbStatus() },
                onRefreshDevices = { viewModel.refreshDevices() },
                onDeviceSelected = { viewModel.selectDevice(it) },
                onLoadAppLinks = { viewModel.loadAppLinks() },
                onReverify = { viewModel.forceReverify(it) },
                onFireIntent = {
                    if (intentUri.isNotBlank()) {
                        val config = IntentConfig(uri = intentUri)
                        viewModel.fireIntent(config)
                    }
                },
                onOpenIntentDialog = { showIntentDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Right Panel - Log Output
            LogPanel(
                logText = logText,
                title = "Log Output",
                onClear = { viewModel.clearLog() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Loading overlay
        LoadingOverlay(
            isLoading = isLoading,
            message = when {
                uiState.isLoadingDevices -> "Detecting devices..."
                uiState.isLoadingAppLinks -> "Loading app links..."
                uiState.isFiringIntent -> "Firing intent..."
                else -> null
            }
        )

        // Intent fire dialog
        if (showIntentDialog) {
            IntentFireDialog(
                onDismiss = { showIntentDialog = false },
                onFire = { config ->
                    viewModel.fireIntent(config)
                    showIntentDialog = false
                },
                initialUri = intentUri
            )
        }
    }
}

/**
 * Left panel with all controls
 */
@Composable
private fun ControlPanel(
    uiState: MainUiState,
    intentUri: String,
    onIntentUriChange: (String) -> Unit,
    onCheckAdb: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDeviceSelected: (com.manjee.linkops.domain.model.Device) -> Unit,
    onLoadAppLinks: () -> Unit,
    onReverify: (String) -> Unit,
    onFireIntent: () -> Unit,
    onOpenIntentDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "LinkOps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // Scrollable content
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: ADB Status
            item {
                AdbStatusSection(
                    status = uiState.adbStatus,
                    onCheck = onCheckAdb
                )
            }

            item { HorizontalDivider() }

            // Section 2: Devices
            item {
                DevicesSection(
                    devices = uiState.devices,
                    selectedDevice = uiState.selectedDevice,
                    isLoading = uiState.isLoadingDevices,
                    onRefresh = onRefreshDevices,
                    onDeviceSelected = onDeviceSelected
                )
            }

            item { HorizontalDivider() }

            // Section 3: Fire Intent
            item {
                FireIntentSection(
                    intentUri = intentUri,
                    onIntentUriChange = onIntentUriChange,
                    selectedDevice = uiState.selectedDevice,
                    isFiring = uiState.isFiringIntent,
                    onFireIntent = onFireIntent,
                    onOpenAdvanced = onOpenIntentDialog
                )
            }

            item { HorizontalDivider() }

            // Section 4: App Links
            item {
                AppLinksSection(
                    appLinks = uiState.appLinks,
                    selectedDevice = uiState.selectedDevice,
                    isLoading = uiState.isLoadingAppLinks,
                    onLoadAppLinks = onLoadAppLinks,
                    onReverify = onReverify
                )
            }
        }
    }
}

/**
 * ADB Status Section
 */
@Composable
private fun AdbStatusSection(
    status: AdbStatus,
    onCheck: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1. ADB Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = onCheck,
                enabled = status !is AdbStatus.Checking
            ) {
                Text("Check ADB")
            }
        }

        // Status indicator
        when (status) {
            is AdbStatus.Unknown -> {
                Text(
                    text = "Status unknown - click Check ADB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is AdbStatus.Checking -> {
                LoadingIndicator(message = "Checking ADB...")
            }
            is AdbStatus.Available -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = LinkOpsColors.Success)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADB Available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LinkOpsColors.Success
                    )
                }
            }
            is AdbStatus.Unavailable -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = LinkOpsColors.Error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADB Unavailable: ${status.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LinkOpsColors.Error
                    )
                }
            }
        }
    }
}

/**
 * Devices Section (Collapsible)
 */
@Composable
private fun DevicesSection(
    devices: List<com.manjee.linkops.domain.model.Device>,
    selectedDevice: com.manjee.linkops.domain.model.Device?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDeviceSelected: (com.manjee.linkops.domain.model.Device) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
                Text(
                    text = "2. Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (devices.isNotEmpty()) {
                    Text(
                        text = "(${devices.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRefresh,
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (devices.isEmpty()) {
                    EmptyState(
                        title = "No devices found",
                        description = "Connect an Android device or start an emulator",
                        icon = Icons.Default.PhoneAndroid,
                        actionLabel = "Refresh Devices",
                        onAction = onRefresh
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        devices.forEach { device ->
                            DeviceCard(
                                device = device,
                                isSelected = selectedDevice?.serialNumber == device.serialNumber,
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * App Links Section (Collapsible)
 */
@Composable
private fun AppLinksSection(
    appLinks: List<com.manjee.linkops.domain.model.AppLink>,
    selectedDevice: com.manjee.linkops.domain.model.Device?,
    isLoading: Boolean,
    onLoadAppLinks: () -> Unit,
    onReverify: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
                Text(
                    text = "4. App Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (appLinks.isNotEmpty()) {
                    Text(
                        text = "(${appLinks.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onLoadAppLinks,
                enabled = !isLoading && selectedDevice != null
            ) {
                Text("Get App Links")
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedDevice == null) {
                    Text(
                        text = "Select a device first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (appLinks.isEmpty()) {
                    if (!isLoading) {
                        EmptyState(
                            title = "No app links found",
                            description = "Click 'Get App Links' to fetch",
                            icon = Icons.Default.Link,
                            actionLabel = "Get App Links",
                            onAction = onLoadAppLinks
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        appLinks.forEach { appLink ->
                            AppLinkCard(
                                appLink = appLink,
                                onReverify = { onReverify(appLink.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fire Intent Section
 */
@Composable
private fun FireIntentSection(
    intentUri: String,
    onIntentUriChange: (String) -> Unit,
    selectedDevice: com.manjee.linkops.domain.model.Device?,
    isFiring: Boolean,
    onFireIntent: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "3. Fire Intent",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = intentUri,
            onValueChange = onIntentUriChange,
            label = { Text("URI") },
            placeholder = { Text("myapp://product/123 or https://example.com/path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = selectedDevice != null
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onFireIntent,
                enabled = !isFiring && selectedDevice != null && intentUri.isNotBlank()
            ) {
                Text("Fire Intent")
            }

            OutlinedButton(
                onClick = onOpenAdvanced,
                enabled = selectedDevice != null
            ) {
                Text("Advanced...")
            }
        }
    }
}
