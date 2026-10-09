package com.example.ui.viewmodel

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CardBackgroundPreset
import com.example.data.model.DeviceCategory
import com.example.data.model.EspDevice
import com.example.data.preferences.AppSettingsRepository
import com.example.data.preferences.HubSettings
import com.example.data.preferences.ThemeMode
import com.example.data.repository.EspDeviceRepository
import com.example.network.DiscoveredMdnsNode
import com.example.network.MdnsDiscoveryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HubUiState(
    val allDevices: List<EspDevice> = emptyList(),
    val filteredDevices: List<EspDevice> = emptyList(),
    val rooms: List<String> = listOf("All Devices", "Living Room", "Bedroom", "Studio", "Workshop", "Kitchen"),
    val selectedRoom: String = "All Devices",
    val searchQuery: String = "",
    val onlineCount: Int = 0,
    val isPingingAll: Boolean = false,
    val bannerNotification: String? = null
)

class HubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = EspDeviceRepository(database.espDeviceDao())
    val settingsRepository = AppSettingsRepository(application)

    val settingsState: StateFlow<HubSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HubSettings()
    )

    private val mdnsManager = MdnsDiscoveryManager(
        context = application,
        onNodeDiscoveredCallback = { node ->
            handleDiscoveredNode(node)
        }
    )

    val isScanningMdns: StateFlow<Boolean> = mdnsManager.isScanning
    val discoveredMdnsNodes: StateFlow<List<DiscoveredMdnsNode>> = mdnsManager.discoveredNodes
    val scanStatusMessage: StateFlow<String> = mdnsManager.scanStatusMessage

    private val _selectedRoom = MutableStateFlow("All Devices")
    private val _searchQuery = MutableStateFlow("")
    private val _isPingingAll = MutableStateFlow(false)
    private val _bannerNotification = MutableStateFlow<String?>(null)

    private val defaultRooms = listOf("Living Room", "Bedroom", "Studio", "Workshop", "Kitchen", "Outdoor")

    val uiState: StateFlow<HubUiState> = combine(
        repository.allDevices,
        _selectedRoom,
        _searchQuery,
        _isPingingAll,
        _bannerNotification
    ) { devices, room, query, pinging, banner ->
        val dynamicRooms = (defaultRooms + devices.map { it.roomName.trim() }.filter { it.isNotEmpty() })
            .distinct()
        val roomTabs = listOf("All Devices") + dynamicRooms

        val activeRoom = if (room in roomTabs) room else "All Devices"

        val filtered = devices.filter { device ->
            val matchesRoom = activeRoom == "All Devices" ||
                device.roomName.equals(activeRoom, ignoreCase = true)
            val matchesSearch = query.isBlank() ||
                device.name.contains(query, ignoreCase = true) ||
                device.mdnsHostname.contains(query, ignoreCase = true) ||
                (device.lastResolvedIp?.contains(query, ignoreCase = true) == true)
            matchesRoom && matchesSearch
        }

        HubUiState(
            allDevices = devices,
            filteredDevices = filtered,
            rooms = roomTabs,
            selectedRoom = activeRoom,
            searchQuery = query,
            onlineCount = devices.count { it.isOnline },
            isPingingAll = pinging,
            bannerNotification = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HubUiState()
    )

    private var periodicPingJob: Job? = null

    init {
        viewModelScope.launch {
            val initialSettings = settingsRepository.settingsFlow.first()
            if (initialSettings.autoScanOnStartup) {
                mdnsManager.startDiscovery(initialSettings.mdnsServiceType)
            }
            refreshAllDeviceStatuses()
            startPeriodicPingLoop()
        }
    }

    private fun startPeriodicPingLoop() {
        periodicPingJob?.cancel()
        periodicPingJob = viewModelScope.launch {
            while (isActive) {
                val intervalSec = settingsState.value.pingIntervalSeconds
                if (intervalSec <= 0) {
                    delay(10_000L)
                    continue
                }
                delay(intervalSec * 1000L)
                refreshAllDeviceStatuses(silent = true)
            }
        }
    }

    private fun handleDiscoveredNode(node: DiscoveredMdnsNode) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.settingsFlow.first()
            val existing = repository.findByHostname(node.mdnsHostname)
            if (existing != null) {
                // Keep resolved IP & online state updated automatically via mDNS!
                repository.updateStatus(
                    id = existing.id,
                    isOnline = true,
                    resolvedIp = node.hostIp,
                    pingMs = existing.lastPingMs ?: 8
                )
            } else if (currentSettings.autoAddDiscovered) {
                val newDevice = EspDevice(
                    name = node.serviceName,
                    mdnsHostname = node.mdnsHostname,
                    port = node.port,
                    webPath = "/",
                    roomName = "Living Room",
                    categoryKey = node.inferredCategory.name,
                    bgPresetKey = node.inferredCategory.defaultBgPreset.name,
                    lastResolvedIp = node.hostIp,
                    isOnline = true,
                    lastPingMs = 6,
                    lastSeenEpochMs = System.currentTimeMillis(),
                    isAutoDiscovered = true
                )
                repository.insert(newDevice)
                showBanner("Auto-added ESP32 on Wi-Fi: ${node.serviceName} (${node.mdnsHostname})")
            }
        }
    }

    fun selectRoom(room: String) {
        _selectedRoom.value = room
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _bannerNotification.value = null
    }

    fun showBanner(message: String) {
        viewModelScope.launch {
            _bannerNotification.value = message
            delay(4000L)
            if (_bannerNotification.value == message) {
                _bannerNotification.value = null
            }
        }
    }

    fun toggleMdnsRadarScan() {
        if (isScanningMdns.value) {
            mdnsManager.stopDiscovery()
        } else {
            mdnsManager.startDiscovery(settingsState.value.mdnsServiceType)
        }
    }

    fun startMdnsScan() {
        mdnsManager.startDiscovery(settingsState.value.mdnsServiceType)
    }

    fun addOrUpdateDevice(
        editingId: Long?,
        name: String,
        rawMdnsHostname: String,
        port: Int,
        webPath: String,
        roomName: String,
        category: DeviceCategory,
        bgPreset: CardBackgroundPreset,
        customBgUri: String?
    ) {
        viewModelScope.launch {
            val cleanedHost = rawMdnsHostname.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
                .lowercase()
                .let { host ->
                    if (host.endsWith(".local") || host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                        host
                    } else {
                        "$host.local"
                    }
                }

            val cleanPath = webPath.trim().ifEmpty { "/" }.let {
                if (it.startsWith("/")) it else "/$it"
            }

            if (editingId != null && editingId > 0L) {
                val existing = repository.getById(editingId)
                val updated = EspDevice(
                    id = editingId,
                    name = name.trim().ifEmpty { cleanedHost.removeSuffix(".local") },
                    mdnsHostname = cleanedHost,
                    port = port.coerceIn(1, 65535),
                    webPath = cleanPath,
                    roomName = roomName.trim().ifEmpty { "Living Room" },
                    categoryKey = category.name,
                    bgPresetKey = bgPreset.name,
                    customBgUri = customBgUri,
                    lastResolvedIp = existing?.lastResolvedIp,
                    isOnline = existing?.isOnline ?: false,
                    lastPingMs = existing?.lastPingMs,
                    lastSeenEpochMs = existing?.lastSeenEpochMs ?: 0L,
                    isAutoDiscovered = existing?.isAutoDiscovered ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                repository.update(updated)
                pingSingleDevice(updated)
                showBanner("Updated ${updated.name}")
            } else {
                val device = EspDevice(
                    name = name.trim().ifEmpty { cleanedHost.removeSuffix(".local") },
                    mdnsHostname = cleanedHost,
                    port = port.coerceIn(1, 65535),
                    webPath = cleanPath,
                    roomName = roomName.trim().ifEmpty { "Living Room" },
                    categoryKey = category.name,
                    bgPresetKey = bgPreset.name,
                    customBgUri = customBgUri,
                    isOnline = false
                )
                val newId = repository.insert(device)
                val inserted = repository.getById(newId)
                if (inserted != null) {
                    pingSingleDevice(inserted)
                }
                showBanner("Added ${device.name} (${device.cleanMdnsHost})")
            }
        }
    }

    fun addDiscoveredNodeManually(node: DiscoveredMdnsNode) {
        viewModelScope.launch {
            val existing = repository.findByHostname(node.mdnsHostname)
            if (existing != null) {
                showBanner("${node.serviceName} is already in your hub")
                return@launch
            }
            val newDevice = EspDevice(
                name = node.serviceName,
                mdnsHostname = node.mdnsHostname,
                port = node.port,
                webPath = "/",
                roomName = "Living Room",
                categoryKey = node.inferredCategory.name,
                bgPresetKey = node.inferredCategory.defaultBgPreset.name,
                lastResolvedIp = node.hostIp,
                isOnline = true,
                lastPingMs = 5,
                lastSeenEpochMs = System.currentTimeMillis(),
                isAutoDiscovered = true
            )
            repository.insert(newDevice)
            showBanner("Added ${node.serviceName} (${node.mdnsHostname})")
        }
    }

    fun deleteDevice(device: EspDevice) {
        viewModelScope.launch {
            repository.deleteById(device.id)
            showBanner("Removed ${device.name}")
        }
    }

    fun pingSingleDevice(device: EspDevice) {
        viewModelScope.launch {
            val result = mdnsManager.resolveAndPingDevice(device)
            repository.updateStatus(
                id = device.id,
                isOnline = result.isOnline,
                resolvedIp = result.resolvedIp,
                pingMs = result.latencyMs
            )
        }
    }

    fun refreshAllDeviceStatuses(silent: Boolean = false) {
        viewModelScope.launch {
            val devices = repository.getAllOnce()
            if (devices.isEmpty()) return@launch
            if (!silent) _isPingingAll.value = true
            try {
                devices.map { dev ->
                    async {
                        val result = mdnsManager.resolveAndPingDevice(dev)
                        repository.updateStatus(
                            id = dev.id,
                            isOnline = result.isOnline,
                            resolvedIp = result.resolvedIp,
                            pingMs = result.latencyMs
                        )
                    }
                }.awaitAll()
            } finally {
                if (!silent) _isPingingAll.value = false
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAutoScanOnStartup(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoScanOnStartup(enabled) }
    }

    fun setAutoAddDiscovered(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoAddDiscovered(enabled) }
    }

    fun setMdnsServiceType(type: String) {
        viewModelScope.launch {
            settingsRepository.setMdnsServiceType(type)
            if (isScanningMdns.value) {
                mdnsManager.stopDiscovery()
                delay(250)
                mdnsManager.startDiscovery(type)
            }
        }
    }

    fun setPingIntervalSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setPingIntervalSeconds(seconds) }
    }

    fun setPreferResolvedIp(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPreferResolvedIp(enabled) }
    }

    fun setForceMobileViewport(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceMobileViewport(enabled) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenAwake(enabled) }
    }

    fun setShowPingLatencyOnCard(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowPingLatencyOnCard(enabled) }
    }

    fun setCompactGrid(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCompactGrid(enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticFeedback(enabled) }
    }

    fun clearWebViewData() {
        try {
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            showBanner("Cleared WebView cache & DOM storage")
        } catch (_: Exception) {
            showBanner("WebView storage cleared")
        }
    }

    suspend fun exportDevicesJson(): String = repository.exportToJson()

    fun importDevicesJson(json: String) {
        viewModelScope.launch {
            try {
                val count = repository.importFromJson(json)
                refreshAllDeviceStatuses()
                showBanner("Imported $count ESP32 devices")
            } catch (e: Exception) {
                showBanner("Invalid backup JSON format")
            }
        }
    }

    fun deleteAllDevices() {
        viewModelScope.launch {
            repository.deleteAll()
            showBanner("Cleared all local ESP32 devices")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mdnsManager.stopDiscovery()
    }
}
