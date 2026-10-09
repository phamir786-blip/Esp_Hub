package com.example

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.EspDevice
import com.example.data.preferences.ThemeMode
import com.example.ui.screens.AddEditDeviceSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceWebAppScreen
import com.example.ui.screens.MdnsRadarScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HubViewModel

enum class HubTab {
    HOME,
    RADAR,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val hubViewModel: HubViewModel = viewModel()
            val settings by hubViewModel.settingsState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            val useDarkTheme = when (settings.themeMode) {
                ThemeMode.AMOLED_DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                EspSmartHubApp(viewModel = hubViewModel)
            }
        }
    }
}

@Composable
fun EspSmartHubApp(viewModel: HubViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isScanningMdns by viewModel.isScanningMdns.collectAsStateWithLifecycle()
    val discoveredNodes by viewModel.discoveredMdnsNodes.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()

    val localView = LocalView.current
    fun triggerHaptic() {
        if (settings.hapticFeedback) {
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    var currentTab by remember { mutableStateOf(HubTab.HOME) }
    var activeWebAppDeviceId by remember { mutableStateOf<Long?>(null) }
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<EspDevice?>(null) }

    val activeWebAppDevice = remember(uiState.allDevices, activeWebAppDeviceId) {
        uiState.allDevices.firstOrNull { it.id == activeWebAppDeviceId }
    }

    // Handle back navigation from Radar/Settings tabs to Home tab
    if (activeWebAppDevice == null && currentTab != HubTab.HOME) {
        BackHandler {
            currentTab = HubTab.HOME
        }
    }

    if (activeWebAppDevice != null) {
        DeviceWebAppScreen(
            device = activeWebAppDevice,
            settings = settings,
            onBack = {
                triggerHaptic()
                activeWebAppDeviceId = null
            },
            onRetryPing = { dev -> viewModel.pingSingleDevice(dev) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == HubTab.HOME,
                        onClick = {
                            triggerHaptic()
                            currentTab = HubTab.HOME
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == HubTab.HOME) {
                                    Icons.Filled.GridView
                                } else {
                                    Icons.Outlined.GridView
                                },
                                contentDescription = "My Home"
                            )
                        },
                        label = { Text("My Home") },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == HubTab.RADAR,
                        onClick = {
                            triggerHaptic()
                            currentTab = HubTab.RADAR
                            viewModel.startMdnsScan()
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == HubTab.RADAR) {
                                    Icons.Filled.Radar
                                } else {
                                    Icons.Outlined.Radar
                                },
                                contentDescription = "mDNS Radar"
                            )
                        },
                        label = {
                            Text(
                                if (discoveredNodes.isNotEmpty()) {
                                    "mDNS (${discoveredNodes.size})"
                                } else {
                                    "mDNS Radar"
                                }
                            )
                        },
                        modifier = Modifier.testTag("nav_radar")
                    )

                    NavigationBarItem(
                        selected = currentTab == HubTab.SETTINGS,
                        onClick = {
                            triggerHaptic()
                            currentTab = HubTab.SETTINGS
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == HubTab.SETTINGS) {
                                    Icons.Filled.Settings
                                } else {
                                    Icons.Outlined.Settings
                                },
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "hub_tab_transition"
                ) { tab ->
                    when (tab) {
                        HubTab.HOME -> {
                            DashboardScreen(
                                uiState = uiState,
                                settings = settings,
                                isScanningMdns = isScanningMdns,
                                discoveredCount = discoveredNodes.size,
                                onSelectRoom = { room ->
                                    triggerHaptic()
                                    viewModel.selectRoom(room)
                                },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onOpenAddDevice = {
                                    triggerHaptic()
                                    editingDevice = null
                                    showAddEditSheet = true
                                },
                                onOpenRadarTab = {
                                    triggerHaptic()
                                    currentTab = HubTab.RADAR
                                    viewModel.startMdnsScan()
                                },
                                onRefreshPings = {
                                    triggerHaptic()
                                    viewModel.refreshAllDeviceStatuses()
                                },
                                onQuickToggleTheme = {
                                    triggerHaptic()
                                    val nextTheme = if (settings.themeMode == ThemeMode.LIGHT) {
                                        ThemeMode.AMOLED_DARK
                                    } else {
                                        ThemeMode.LIGHT
                                    }
                                    viewModel.setThemeMode(nextTheme)
                                },
                                onOpenDeviceWebApp = { device ->
                                    triggerHaptic()
                                    activeWebAppDeviceId = device.id
                                },
                                onEditDevice = { device ->
                                    triggerHaptic()
                                    editingDevice = device
                                    showAddEditSheet = true
                                },
                                onPingDevice = { device ->
                                    triggerHaptic()
                                    viewModel.pingSingleDevice(device)
                                },
                                onDeleteDevice = { device ->
                                    triggerHaptic()
                                    viewModel.deleteDevice(device)
                                }
                            )
                        }

                        HubTab.RADAR -> {
                            MdnsRadarScreen(
                                isScanning = isScanningMdns,
                                scanStatusMessage = scanStatusMessage,
                                discoveredNodes = discoveredNodes,
                                savedDevices = uiState.allDevices,
                                autoAddEnabled = settings.autoAddDiscovered,
                                serviceType = settings.mdnsServiceType,
                                onToggleScan = {
                                    triggerHaptic()
                                    viewModel.toggleMdnsRadarScan()
                                },
                                onToggleAutoAdd = { enabled ->
                                    triggerHaptic()
                                    viewModel.setAutoAddDiscovered(enabled)
                                },
                                onAddNode = { node ->
                                    triggerHaptic()
                                    viewModel.addDiscoveredNodeManually(node)
                                },
                                onOpenManualAdd = {
                                    triggerHaptic()
                                    editingDevice = null
                                    showAddEditSheet = true
                                },
                                onOpenSavedDevice = { device ->
                                    triggerHaptic()
                                    activeWebAppDeviceId = device.id
                                }
                            )
                        }

                        HubTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                deviceCount = uiState.allDevices.size,
                                onSelectTheme = { mode ->
                                    triggerHaptic()
                                    viewModel.setThemeMode(mode)
                                },
                                onToggleAutoScan = { viewModel.setAutoScanOnStartup(it) },
                                onToggleAutoAdd = { viewModel.setAutoAddDiscovered(it) },
                                onChangeServiceType = { viewModel.setMdnsServiceType(it) },
                                onChangePingInterval = { viewModel.setPingIntervalSeconds(it) },
                                onTogglePreferResolvedIp = { viewModel.setPreferResolvedIp(it) },
                                onToggleForceMobileViewport = { viewModel.setForceMobileViewport(it) },
                                onToggleKeepScreenAwake = { viewModel.setKeepScreenAwake(it) },
                                onToggleShowPingLatency = { viewModel.setShowPingLatencyOnCard(it) },
                                onToggleCompactGrid = { viewModel.setCompactGrid(it) },
                                onToggleHapticFeedback = { viewModel.setHapticFeedback(it) },
                                onClearWebViewCache = { viewModel.clearWebViewData() },
                                onExportJson = { viewModel.exportDevicesJson() },
                                onImportJson = { viewModel.importDevicesJson(it) },
                                onClearAllDevices = { viewModel.deleteAllDevices() },
                                onShowMessage = { viewModel.showBanner(it) }
                            )
                        }
                    }
                }

                // Floating Toast / Notification Banner
                AnimatedVisibility(
                    visible = uiState.bannerNotification != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Surface(
                        onClick = { viewModel.clearBanner() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.bannerNotification.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddEditSheet) {
        AddEditDeviceSheet(
            editingDevice = editingDevice,
            availableRooms = uiState.rooms,
            onDismiss = {
                showAddEditSheet = false
                editingDevice = null
            },
            onSave = { id, name, host, port, path, room, cat, bgPreset, customUri ->
                viewModel.addOrUpdateDevice(
                    editingId = id,
                    name = name,
                    rawMdnsHostname = host,
                    port = port,
                    webPath = path,
                    roomName = room,
                    category = cat,
                    bgPreset = bgPreset,
                    customBgUri = customUri
                )
            }
        )
    }
}
