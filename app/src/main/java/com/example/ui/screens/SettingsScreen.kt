package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.preferences.HubSettings
import com.example.data.preferences.ThemeMode
import kotlinx.coroutines.launch

private const val MIT_FREE_LICENSE_TEXT = """
MIT Free & Open Source License
Version Nightly 1.0.0 — ESP32 Smart Hub

Copyright (c) 2026 ESP32 Smart Hub Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
"""

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: HubSettings,
    deviceCount: Int,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleAutoScan: (Boolean) -> Unit,
    onToggleAutoAdd: (Boolean) -> Unit,
    onChangeServiceType: (String) -> Unit,
    onChangePingInterval: (Int) -> Unit,
    onTogglePreferResolvedIp: (Boolean) -> Unit,
    onToggleForceMobileViewport: (Boolean) -> Unit,
    onToggleKeepScreenAwake: (Boolean) -> Unit,
    onToggleShowPingLatency: (Boolean) -> Unit,
    onToggleCompactGrid: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    onClearWebViewCache: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: (String) -> Unit,
    onClearAllDevices: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLicenseDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings & Engine",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Configure AMOLED theme, mDNS auto-discovery, frameless WebView, and local storage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Theme & Appearance Section
        item {
            SettingsSectionCard(
                title = "Appearance & AMOLED Theme",
                icon = Icons.Filled.DarkMode
            ) {
                Text(
                    text = "Theme Mode (Dark theme uses pure #000000 AMOLED black)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeOptionButton(
                        label = "AMOLED Dark",
                        icon = Icons.Filled.DarkMode,
                        selected = settings.themeMode == ThemeMode.AMOLED_DARK,
                        onClick = { onSelectTheme(ThemeMode.AMOLED_DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_amoled_button")
                    )
                    ThemeModeOptionButton(
                        label = "Light",
                        icon = Icons.Filled.LightMode,
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onSelectTheme(ThemeMode.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_button")
                    )
                    ThemeModeOptionButton(
                        label = "System",
                        icon = Icons.Filled.SettingsBrightness,
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_system_button")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                SettingsSwitchRow(
                    title = "Show Ping Latency (ms) on Cards",
                    subtitle = "Displays real-time local Wi-Fi response time on online ESP32 cards",
                    checked = settings.showPingLatencyOnCard,
                    onCheckedChange = onToggleShowPingLatency
                )

                SettingsSwitchRow(
                    title = "Compact Card Density",
                    subtitle = "Fit more ESP32 devices on screen simultaneously",
                    checked = settings.compactGrid,
                    onCheckedChange = onToggleCompactGrid
                )

                SettingsSwitchRow(
                    title = "Tactile Haptic Feedback",
                    subtitle = "Subtle vibration when touching device cards and controls",
                    checked = settings.hapticFeedback,
                    onCheckedChange = onToggleHapticFeedback
                )
            }
        }

        // 2. mDNS & Auto-Discovery Section
        item {
            SettingsSectionCard(
                title = "mDNS Discovery & Status Engine",
                icon = Icons.Filled.Dns
            ) {
                SettingsSwitchRow(
                    title = "Auto-Scan Home Wi-Fi on Launch",
                    subtitle = "Automatically starts NSD multicast listener when the app opens",
                    checked = settings.autoScanOnStartup,
                    onCheckedChange = onToggleAutoScan
                )

                SettingsSwitchRow(
                    title = "Auto-Add Discovered ESP32s",
                    subtitle = "Adds newly discovered .local devices directly to your dashboard",
                    checked = settings.autoAddDiscovered,
                    onCheckedChange = onToggleAutoAdd
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "mDNS Service Type Filter",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))

                val presetServiceTypes = listOf("_http._tcp.", "_wled._tcp.", "_esphomelib._tcp.", "_ws._tcp.")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetServiceTypes.forEach { sType ->
                        FilterChip(
                            selected = settings.mdnsServiceType.equals(sType, ignoreCase = true),
                            onClick = { onChangeServiceType(sType) },
                            label = { Text(sType) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Background Status Ping Interval",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))

                val pingOptions = listOf(10 to "10s (Fast)", 15 to "15s", 30 to "30s", 60 to "60s", 0 to "Manual Only")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pingOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.pingIntervalSeconds == sec,
                            onClick = { onChangePingInterval(sec) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 3. Frameless WebView Engine Section
        item {
            SettingsSectionCard(
                title = "Frameless Native WebView Engine",
                icon = Icons.Filled.Web
            ) {
                SettingsSwitchRow(
                    title = "Smart mDNS-to-IP Routing",
                    subtitle = "Saves .local permanently so devices are never lost, but routes WebView via resolved IP to bypass Android Private DNS blocks",
                    checked = settings.preferResolvedIp,
                    onCheckedChange = onTogglePreferResolvedIp
                )

                SettingsSwitchRow(
                    title = "Force Native Mobile Viewport",
                    subtitle = "Locks mobile scaling and disables browser text-selection callouts without spoofing your real ESP32 UI",
                    checked = settings.forceMobileViewport,
                    onCheckedChange = onToggleForceMobileViewport
                )

                SettingsSwitchRow(
                    title = "Keep Screen Awake in Device View",
                    subtitle = "Prevents screen from dimming while controlling an ESP32 web-app",
                    checked = settings.keepScreenAwake,
                    onCheckedChange = onToggleKeepScreenAwake
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onClearWebViewCache,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear WebView Cache & Local DOM Storage")
                }
            }
        }

        // 4. Local Storage & Backup Section
        item {
            SettingsSectionCard(
                title = "Local Database ($deviceCount Saved Devices)",
                icon = Icons.Filled.Memory
            ) {
                Text(
                    text = "100% Local On-Device Storage (Room SQLite). Zero cloud servers or external accounts used.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = onExportJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("esp32_hub_backup", json))
                                onShowMessage("Copied $deviceCount devices JSON backup to clipboard")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON")
                    }
                }

                if (deviceCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove All Local ESP32 Devices")
                    }
                }
            }
        }

        // 5. About, Version Info (Nightly 1.0.0) & Free License Section
        item {
            SettingsSectionCard(
                title = "About & Version Info",
                icon = Icons.Filled.Info
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESP32 Smart Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Local mDNS Tuya-Style Web-App Launcher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("version_badge_text")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                AboutDetailRow(label = "Release Version", value = "Nightly 1.0.0")
                AboutDetailRow(label = "Discovery Protocol", value = "mDNS / DNS-SD (.local)")
                AboutDetailRow(label = "Storage Engine", value = "100% Local Room SQLite")
                AboutDetailRow(label = "License", value = "Free & Open Source (MIT)")

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Free Software License (MIT License)",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Free to use, modify, and distribute with zero telemetry or cloud lock-in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showLicenseDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_free_license_button")
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Read Full Free License (Nightly 1.0.0)")
                        }
                    }
                }
            }
        }
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            icon = {
                Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("MIT Free & Open Source License")
            },
            text = {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = MIT_FREE_LICENSE_TEXT.trim(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLicenseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import ESP32 Devices JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("Paste exported JSON array") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportJson(importJsonInput)
                        showImportDialog = false
                        importJsonInput = ""
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Remove All Local Devices?") },
            text = {
                Text("This will clear all saved ESP32 cards from local storage. Active ESP32s on your Wi-Fi can still be re-discovered via mDNS.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllDevices()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeModeOptionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AboutDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
