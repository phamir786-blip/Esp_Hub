package com.example.data.repository

import com.example.data.local.EspDeviceDao
import com.example.data.model.EspDevice
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class EspDeviceRepository(private val dao: EspDeviceDao) {

    val allDevices: Flow<List<EspDevice>> = dao.observeAllDevices()

    suspend fun getAllOnce(): List<EspDevice> = dao.getAllDevicesOnce()

    suspend fun findByHostname(hostname: String): EspDevice? = dao.findByHostname(hostname)

    suspend fun getById(id: Long): EspDevice? = dao.getDeviceById(id)

    suspend fun insert(device: EspDevice): Long = dao.insertDevice(device)

    suspend fun update(device: EspDevice) = dao.updateDevice(device)

    suspend fun updateStatus(
        id: Long,
        isOnline: Boolean,
        resolvedIp: String?,
        pingMs: Int?
    ) {
        val now = if (isOnline) System.currentTimeMillis() else 0L
        val existing = dao.getDeviceById(id)
        val seenTime = if (isOnline) now else (existing?.lastSeenEpochMs ?: 0L)
        dao.updateConnectionStatus(
            id = id,
            isOnline = isOnline,
            resolvedIp = resolvedIp,
            pingMs = pingMs,
            seenEpochMs = seenTime
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteDeviceById(id)

    suspend fun deleteAll() = dao.deleteAllDevices()

    suspend fun exportToJson(): String {
        val list = dao.getAllDevicesOnce()
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("mdnsHostname", item.mdnsHostname)
                put("port", item.port)
                put("webPath", item.webPath)
                put("roomName", item.roomName)
                put("categoryKey", item.categoryKey)
                put("bgPresetKey", item.bgPresetKey)
                put("customBgUri", item.customBgUri ?: "")
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Int {
        val array = JSONArray(jsonString)
        var importedCount = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val hostname = obj.optString("mdnsHostname").trim()
            if (hostname.isNotEmpty()) {
                val existing = dao.findByHostname(hostname)
                val customUri = obj.optString("customBgUri").takeIf { it.isNotBlank() }
                val device = EspDevice(
                    id = existing?.id ?: 0L,
                    name = obj.optString("name", hostname.removeSuffix(".local")),
                    mdnsHostname = hostname,
                    port = obj.optInt("port", 80),
                    webPath = obj.optString("webPath", "/"),
                    roomName = obj.optString("roomName", "Living Room"),
                    categoryKey = obj.optString("categoryKey", "LIGHT"),
                    bgPresetKey = obj.optString("bgPresetKey", "TUYA_EMBER"),
                    customBgUri = customUri,
                    lastResolvedIp = existing?.lastResolvedIp,
                    isOnline = existing?.isOnline ?: false
                )
                dao.insertDevice(device)
                importedCount++
            }
        }
        return importedCount
    }
}
