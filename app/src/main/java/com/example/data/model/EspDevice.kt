package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "esp_devices")
data class EspDevice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mdnsHostname: String,
    val port: Int = 80,
    val webPath: String = "/",
    val roomName: String = "Living Room",
    val categoryKey: String = DeviceCategory.LIGHT.name,
    val bgPresetKey: String = CardBackgroundPreset.TUYA_EMBER.name,
    val customBgUri: String? = null,
    val lastResolvedIp: String? = null,
    val isOnline: Boolean = false,
    val lastPingMs: Int? = null,
    val lastSeenEpochMs: Long = 0L,
    val isAutoDiscovered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val category: DeviceCategory
        get() = DeviceCategory.fromKey(categoryKey)

    val bgPreset: CardBackgroundPreset
        get() = CardBackgroundPreset.fromKey(bgPresetKey)

    val cleanMdnsHost: String
        get() {
            val trimmed = mdnsHostname.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
                .lowercase()
            return if (trimmed.endsWith(".local") || trimmed.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                trimmed
            } else {
                "$trimmed.local"
            }
        }

    fun buildTargetUrl(preferResolvedIp: Boolean): String {
        val host = if (preferResolvedIp && !lastResolvedIp.isNullOrBlank()) {
            lastResolvedIp
        } else {
            cleanMdnsHost
        }
        val portSuffix = if (port == 80) "" else ":$port"
        val normalizedPath = if (webPath.startsWith("/")) webPath else "/$webPath"
        return "http://$host$portSuffix$normalizedPath"
    }
}

enum class DeviceCategory(
    val displayName: String,
    val icon: ImageVector,
    val accentColor: Color,
    val defaultBgPreset: CardBackgroundPreset
) {
    LIGHT(
        displayName = "Smart Light",
        icon = Icons.Filled.Lightbulb,
        accentColor = Color(0xFFFFB300),
        defaultBgPreset = CardBackgroundPreset.SUNSET_GOLD
    ),
    MUSIC(
        displayName = "Music & Audio",
        icon = Icons.Filled.MusicNote,
        accentColor = Color(0xFFFF4081),
        defaultBgPreset = CardBackgroundPreset.AURORA_VIOLET
    ),
    LED_STRIP(
        displayName = "Ambient LED / WLED",
        icon = Icons.Filled.AutoAwesome,
        accentColor = Color(0xFF00E5FF),
        defaultBgPreset = CardBackgroundPreset.CYBER_CYAN
    ),
    FAN_CLIMATE(
        displayName = "Climate & Fan",
        icon = Icons.Filled.AcUnit,
        accentColor = Color(0xFF29B6F6),
        defaultBgPreset = CardBackgroundPreset.OCEAN_DEEP
    ),
    SWITCH_RELAY(
        displayName = "Smart Switch / Relay",
        icon = Icons.Filled.PowerSettingsNew,
        accentColor = Color(0xFFFF5A1F),
        defaultBgPreset = CardBackgroundPreset.TUYA_EMBER
    ),
    SENSOR(
        displayName = "Environment Sensor",
        icon = Icons.Filled.Sensors,
        accentColor = Color(0xFF00E676),
        defaultBgPreset = CardBackgroundPreset.EMERALD_MATRIX
    ),
    SPEAKER(
        displayName = "Smart Speaker / DAC",
        icon = Icons.Filled.Speaker,
        accentColor = Color(0xFFAB47BC),
        defaultBgPreset = CardBackgroundPreset.AURORA_VIOLET
    ),
    CAMERA(
        displayName = "ESP32-CAM Node",
        icon = Icons.Filled.Videocam,
        accentColor = Color(0xFF26C6DA),
        defaultBgPreset = CardBackgroundPreset.OCEAN_DEEP
    ),
    GARAGE(
        displayName = "Garage & Access",
        icon = Icons.Filled.Garage,
        accentColor = Color(0xFFFF7043),
        defaultBgPreset = CardBackgroundPreset.TUYA_EMBER
    ),
    ESP_NODE(
        displayName = "ESP32 Controller",
        icon = Icons.Filled.DeveloperBoard,
        accentColor = Color(0xFF00E5FF),
        defaultBgPreset = CardBackgroundPreset.OBSIDIAN_SLATE
    );

    companion object {
        fun fromKey(key: String): DeviceCategory =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: ESP_NODE

        fun inferFromName(nameOrHost: String): DeviceCategory {
            val lower = nameOrHost.lowercase()
            return when {
                lower.contains("wled") || lower.contains("strip") || lower.contains("rgb") || lower.contains("neon") -> LED_STRIP
                lower.contains("light") || lower.contains("lamp") || lower.contains("bulb") || lower.contains("dimmer") -> LIGHT
                lower.contains("music") || lower.contains("audio") || lower.contains("amp") || lower.contains("player") -> MUSIC
                lower.contains("speaker") || lower.contains("dac") || lower.contains("radio") -> SPEAKER
                lower.contains("fan") || lower.contains("ac") || lower.contains("climate") || lower.contains("hvac") || lower.contains("heater") -> FAN_CLIMATE
                lower.contains("cam") || lower.contains("stream") -> CAMERA
                lower.contains("sensor") || lower.contains("temp") || lower.contains("humid") || lower.contains("air") || lower.contains("weather") -> SENSOR
                lower.contains("garage") || lower.contains("door") || lower.contains("gate") || lower.contains("lock") -> GARAGE
                lower.contains("switch") || lower.contains("relay") || lower.contains("plug") || lower.contains("socket") -> SWITCH_RELAY
                else -> ESP_NODE
            }
        }
    }
}

enum class CardBackgroundPreset(
    val displayName: String,
    val startColor: Color,
    val endColor: Color,
    val glowColor: Color
) {
    TUYA_EMBER(
        displayName = "Tuya Ember",
        startColor = Color(0xFF2B0F06),
        endColor = Color(0xFF120806),
        glowColor = Color(0xFFFF5A1F)
    ),
    CYBER_CYAN(
        displayName = "Cyber Cyan",
        startColor = Color(0xFF04222C),
        endColor = Color(0xFF060E14),
        glowColor = Color(0xFF00E5FF)
    ),
    AURORA_VIOLET(
        displayName = "Aurora Music",
        startColor = Color(0xFF230B36),
        endColor = Color(0xFF0B0714),
        glowColor = Color(0xFFD500F9)
    ),
    SUNSET_GOLD(
        displayName = "Warm Glow",
        startColor = Color(0xFF2D1C04),
        endColor = Color(0xFF120C04),
        glowColor = Color(0xFFFFB300)
    ),
    EMERALD_MATRIX(
        displayName = "Emerald Eco",
        startColor = Color(0xFF042619),
        endColor = Color(0xFF05110C),
        glowColor = Color(0xFF00E676)
    ),
    OCEAN_DEEP(
        displayName = "Deep Breeze",
        startColor = Color(0xFF071B34),
        endColor = Color(0xFF060C17),
        glowColor = Color(0xFF2979FF)
    ),
    OBSIDIAN_SLATE(
        displayName = "AMOLED Stealth",
        startColor = Color(0xFF121722),
        endColor = Color(0xFF070A0F),
        glowColor = Color(0xFF64748B)
    ),
    CUSTOM_IMAGE(
        displayName = "Custom Photo",
        startColor = Color(0xFF10141D),
        endColor = Color(0xFF05070A),
        glowColor = Color(0xFFFF5A1F)
    );

    companion object {
        fun fromKey(key: String): CardBackgroundPreset =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: TUYA_EMBER
    }
}
