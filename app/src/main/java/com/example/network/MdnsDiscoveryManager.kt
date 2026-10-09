package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.example.data.model.DeviceCategory
import com.example.data.model.EspDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoveredMdnsNode(
    val serviceName: String,
    val mdnsHostname: String,
    val hostIp: String,
    val port: Int,
    val serviceType: String,
    val inferredCategory: DeviceCategory,
    val discoveredAtMs: Long = System.currentTimeMillis()
)

data class PingResult(
    val isOnline: Boolean,
    val resolvedIp: String?,
    val latencyMs: Int?
)

class MdnsDiscoveryManager(
    context: Context,
    private val onNodeDiscoveredCallback: ((DiscoveredMdnsNode) -> Unit)? = null
) {
    private val appContext = context.applicationContext
    private val nsdManager: NsdManager? =
        appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager: WifiManager? =
        appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val isResolveBusy = AtomicBoolean(false)
    private val pendingResolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredNodes = MutableStateFlow<List<DiscoveredMdnsNode>>(emptyList())
    val discoveredNodes: StateFlow<List<DiscoveredMdnsNode>> = _discoveredNodes.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("Idle")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1800, TimeUnit.MILLISECONDS)
        .readTimeout(1800, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    fun startDiscovery(serviceType: String = "_http._tcp.") {
        if (_isScanning.value) return
        val manager = nsdManager ?: run {
            _scanStatusMessage.value = "NSD service unavailable on this device"
            return
        }

        acquireMulticastLock()
        val normalizedType = normalizeServiceType(serviceType)
        _scanStatusMessage.value = "Scanning Wi-Fi for $normalizedType mDNS nodes…"

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
                _scanStatusMessage.value = "Listening for ESP32 mDNS broadcasts ($regType)"
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                enqueueResolve(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // Keep discovered history in radar list so user can still inspect or add it
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
                _scanStatusMessage.value = "mDNS scan paused"
                releaseMulticastLock()
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
                _scanStatusMessage.value = "mDNS scan start failed (code $errorCode)"
                releaseMulticastLock()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
                releaseMulticastLock()
            }
        }

        discoveryListener = listener
        try {
            manager.discoverServices(normalizedType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            _isScanning.value = false
            _scanStatusMessage.value = "Unable to start mDNS scan: ${e.localizedMessage ?: "error"}"
            releaseMulticastLock()
        }
    }

    fun stopDiscovery() {
        val manager = nsdManager ?: return
        val listener = discoveryListener ?: return
        discoveryListener = null
        try {
            manager.stopServiceDiscovery(listener)
        } catch (_: Exception) {
        } finally {
            _isScanning.value = false
            releaseMulticastLock()
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo) {
        pendingResolveQueue.offer(serviceInfo)
        processNextResolve()
    }

    private fun processNextResolve() {
        val manager = nsdManager ?: return
        if (!isResolveBusy.compareAndSet(false, true)) return
        val nextService = pendingResolveQueue.poll()
        if (nextService == null) {
            isResolveBusy.set(false)
            return
        }

        try {
            manager.resolveService(nextService, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    isResolveBusy.set(false)
                    processNextResolve()
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                    handleResolvedService(resolvedInfo)
                    isResolveBusy.set(false)
                    processNextResolve()
                }
            })
        } catch (_: Exception) {
            isResolveBusy.set(false)
            processNextResolve()
        }
    }

    private fun handleResolvedService(resolvedInfo: NsdServiceInfo) {
        val hostAddress: InetAddress? = resolvedInfo.host
        val ip = hostAddress?.hostAddress ?: return
        val rawServiceName = resolvedInfo.serviceName?.trim().orEmpty()
        val rawHostName = hostAddress.hostName?.trim().orEmpty()

        val sanitizedServiceSlug = rawServiceName
            .lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .trim('-')
            .ifEmpty { "esp32-node" }

        val mdnsHost = when {
            rawHostName.endsWith(".local", ignoreCase = true) -> rawHostName.lowercase()
            rawHostName.endsWith(".local.", ignoreCase = true) -> rawHostName.dropLast(1).lowercase()
            else -> "$sanitizedServiceSlug.local"
        }

        val port = if (resolvedInfo.port > 0) resolvedInfo.port else 80
        val displayTitle = rawServiceName.ifEmpty {
            mdnsHost.removeSuffix(".local").replace('-', ' ').replaceFirstChar { it.uppercase() }
        }

        val node = DiscoveredMdnsNode(
            serviceName = displayTitle,
            mdnsHostname = mdnsHost,
            hostIp = ip,
            port = port,
            serviceType = resolvedInfo.serviceType ?: "_http._tcp.",
            inferredCategory = DeviceCategory.inferFromName("$displayTitle $mdnsHost")
        )

        _discoveredNodes.update { current ->
            val filtered = current.filterNot {
                it.mdnsHostname.equals(node.mdnsHostname, ignoreCase = true) ||
                    (it.hostIp == node.hostIp && it.port == node.port)
            }
            listOf(node) + filtered
        }

        _scanStatusMessage.value = "Found ${node.serviceName} (${node.mdnsHostname})"
        scope.launch {
            onNodeDiscoveredCallback?.invoke(node)
        }
    }

    suspend fun resolveAndPingDevice(device: EspDevice): PingResult = withContext(Dispatchers.IO) {
        val cleanHost = device.cleanMdnsHost
        var resolvedIp: String? = null

        // 1. Try resolving mDNS hostname via InetAddress
        try {
            val addresses = InetAddress.getAllByName(cleanHost)
            val ipv4 = addresses.firstOrNull { it is Inet4Address } ?: addresses.firstOrNull()
            if (ipv4 != null && !ipv4.hostAddress.isNullOrBlank()) {
                resolvedIp = ipv4.hostAddress
            }
        } catch (_: Exception) {
            // Also check if our live NSD radar already resolved this hostname
            val radarMatch = _discoveredNodes.value.firstOrNull {
                it.mdnsHostname.equals(cleanHost, ignoreCase = true)
            }
            if (radarMatch != null) {
                resolvedIp = radarMatch.hostIp
            }
        }

        val targetHost = resolvedIp ?: device.lastResolvedIp ?: cleanHost
        val targetPort = if (device.port > 0) device.port else 80

        // 2. Measure TCP socket + HTTP reachability
        val startNs = System.nanoTime()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetHost, targetPort), 1600)
            }
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
            return@withContext PingResult(
                isOnline = true,
                resolvedIp = resolvedIp ?: device.lastResolvedIp,
                latencyMs = elapsedMs
            )
        } catch (_: Exception) {
            // Fallback to lightweight HTTP HEAD request in case raw socket was filtered
            try {
                val url = "http://$targetHost:$targetPort${device.webPath}"
                val req = Request.Builder().url(url).head().build()
                httpClient.newCall(req).execute().use {
                    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
                    return@withContext PingResult(
                        isOnline = true,
                        resolvedIp = resolvedIp ?: device.lastResolvedIp,
                        latencyMs = elapsedMs
                    )
                }
            } catch (_: Exception) {
                return@withContext PingResult(
                    isOnline = false,
                    resolvedIp = resolvedIp ?: device.lastResolvedIp,
                    latencyMs = null
                )
            }
        }
    }

    private fun normalizeServiceType(raw: String): String {
        val trimmed = raw.trim().ifEmpty { "_http._tcp." }
        return if (trimmed.endsWith(".")) trimmed else "$trimmed."
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("esp32_smart_hub_mdns")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
        }
    }
}
