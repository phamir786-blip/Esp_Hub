package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EspDevice
import com.example.data.preferences.HubSettings
import com.example.data.preferences.ThemeMode
import com.example.ui.components.EspDeviceCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OnlineEmerald
import com.example.ui.viewmodel.HubUiState

@Composable
fun DashboardScreen(
    uiState: HubUiState,
    settings: HubSettings,
    isScanningMdns: Boolean,
    discoveredCount: Int,
    onSelectRoom: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenAddDevice: () -> Unit,
    onOpenRadarTab: () -> Unit,
    onRefreshPings: () -> Unit,
    onQuickToggleTheme: () -> Unit,
    onOpenDeviceWebApp: (EspDevice) -> Unit,
    onEditDevice: (EspDevice) -> Unit,
    onPingDevice: (EspDevice) -> Unit,
    onDeleteDevice: (EspDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gridColumns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            maxWidth < 350.dp -> 1
            else -> 2
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Tuya-Style Smart Hub Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ESP32 Smart Hub",
                            style = if (maxWidth < 400.dp) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (maxWidth >= 400.dp) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = OnlineEmerald.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, OnlineEmerald.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(OnlineEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${uiState.onlineCount}/${uiState.allDevices.size} Online",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnlineEmerald
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenRadarTab() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radar,
                            contentDescription = null,
                            tint = if (isScanningMdns) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isScanningMdns) {
                                "mDNS Auto-Scan Active ($discoveredCount found on Wi-Fi)"
                            } else {
                                "Local mDNS Hub • Tap to scan Wi-Fi"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isScanningMdns) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onQuickToggleTheme,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.themeMode == ThemeMode.LIGHT) {
                                Icons.Filled.DarkMode
                            } else {
                                Icons.Filled.LightMode
                            },
                            contentDescription = "Switch Theme (Light / AMOLED Dark)"
                        )
                    }

                    IconButton(
                        onClick = onRefreshPings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_all_pings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Ping & Resolve All ESP32 Devices"
                        )
                    }

                    Surface(
                        onClick = onOpenAddDevice,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("top_add_device_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add ESP32 Device",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Search bar (when user has multiple devices or active search)
            AnimatedVisibility(
                visible = uiState.allDevices.size >= 2 || uiState.searchQuery.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Filter by name or .local address…") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .testTag("search_devices_input")
                )
            }

            // Tuya-Style Room Category Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.rooms) { room ->
                    val isSelected = uiState.selectedRoom.equals(room, ignoreCase = true)
                    Surface(
                        onClick = { onSelectRoom(room) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier.testTag("room_tab_$room")
                    ) {
                        Text(
                            text = room,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Device Card Grid or Empty State
            if (uiState.filteredDevices.isEmpty()) {
                EmptyHubState(
                    hasAnyDevicesAtAll = uiState.allDevices.isNotEmpty(),
                    isScanningMdns = isScanningMdns,
                    onAddManual = onOpenAddDevice,
                    onOpenRadar = onOpenRadarTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 6.dp,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("device_cards_grid")
                ) {
                    items(
                        items = uiState.filteredDevices,
                        key = { it.id }
                    ) { device ->
                        EspDeviceCard(
                            device = device,
                            showPingLatency = settings.showPingLatencyOnCard,
                            compactMode = settings.compactGrid,
                            onOpenWebApp = onOpenDeviceWebApp,
                            onEditDevice = onEditDevice,
                            onPingDevice = onPingDevice,
                            onDeleteDevice = onDeleteDevice,
                            modifier = Modifier.animateItem()
                        )
                    }

                    // Subtle bottom "Add ESP32 (.local)" tile
                    item(span = { GridItemSpan(1) }) {
                        Surface(
                            onClick = onOpenAddDevice,
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (settings.compactGrid) 152.dp else 184.dp)
                                .testTag("grid_add_device_tile")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Add ESP32",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "mDNS .local",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHubState(
    hasAnyDevicesAtAll: Boolean,
    isScanningMdns: Boolean,
    onAddManual: () -> Unit,
    onOpenRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.size(82.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Router,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (hasAnyDevicesAtAll) "No Devices in This Room" else "Ready for Your ESP32 Devices",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasAnyDevicesAtAll) {
                "No ESP32 nodes match the selected room or filter."
            } else if (isScanningMdns) {
                "Auto-scanning your home Wi-Fi for ESP32 mDNS (.local) broadcasts… Any discovered ESP32 will appear here automatically, or add one manually below."
            } else {
                "Add your ESP32 devices using their .local mDNS hostname so your hub never loses them when DHCP IPs change, then tap any card to open its full web-app."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddManual,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp)
                .testTag("empty_add_device_button")
        ) {
            Icon(Icons.Filled.Dns, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add ESP32 via mDNS (.local)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        FilledTonalButton(
            onClick = onOpenRadar,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("empty_open_radar_button")
        ) {
            Icon(Icons.Filled.Radar, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Wi-Fi mDNS Auto-Scanner")
        }
    }
}
