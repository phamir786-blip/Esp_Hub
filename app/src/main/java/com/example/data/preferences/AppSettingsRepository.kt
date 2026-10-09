package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "esp32_hub_settings")

enum class ThemeMode(val label: String) {
    AMOLED_DARK("AMOLED Dark (#000000)"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

data class HubSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val autoScanOnStartup: Boolean = true,
    val autoAddDiscovered: Boolean = true,
    val mdnsServiceType: String = "_http._tcp.",
    val pingIntervalSeconds: Int = 15,
    val preferResolvedIp: Boolean = true,
    val forceMobileViewport: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val showPingLatencyOnCard: Boolean = true,
    val compactGrid: Boolean = false,
    val hapticFeedback: Boolean = true
)

class AppSettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SCAN_STARTUP = booleanPreferencesKey("auto_scan_startup")
        val AUTO_ADD_DISCOVERED = booleanPreferencesKey("auto_add_discovered")
        val MDNS_SERVICE_TYPE = stringPreferencesKey("mdns_service_type")
        val PING_INTERVAL_SEC = intPreferencesKey("ping_interval_sec")
        val PREFER_RESOLVED_IP = booleanPreferencesKey("prefer_resolved_ip")
        val FORCE_MOBILE_VIEWPORT = booleanPreferencesKey("force_mobile_viewport")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val SHOW_PING_LATENCY = booleanPreferencesKey("show_ping_latency")
        val COMPACT_GRID = booleanPreferencesKey("compact_grid")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settingsFlow: Flow<HubSettings> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.AMOLED_DARK.name
        val mode = ThemeMode.entries.firstOrNull { it.name == modeStr } ?: ThemeMode.AMOLED_DARK
        HubSettings(
            themeMode = mode,
            autoScanOnStartup = prefs[Keys.AUTO_SCAN_STARTUP] ?: true,
            autoAddDiscovered = prefs[Keys.AUTO_ADD_DISCOVERED] ?: true,
            mdnsServiceType = prefs[Keys.MDNS_SERVICE_TYPE] ?: "_http._tcp.",
            pingIntervalSeconds = prefs[Keys.PING_INTERVAL_SEC] ?: 15,
            preferResolvedIp = prefs[Keys.PREFER_RESOLVED_IP] ?: true,
            forceMobileViewport = prefs[Keys.FORCE_MOBILE_VIEWPORT] ?: true,
            keepScreenAwake = prefs[Keys.KEEP_SCREEN_AWAKE] ?: true,
            showPingLatencyOnCard = prefs[Keys.SHOW_PING_LATENCY] ?: true,
            compactGrid = prefs[Keys.COMPACT_GRID] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoScanOnStartup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SCAN_STARTUP] = enabled }
    }

    suspend fun setAutoAddDiscovered(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ADD_DISCOVERED] = enabled }
    }

    suspend fun setMdnsServiceType(serviceType: String) {
        val cleaned = serviceType.trim().ifEmpty { "_http._tcp." }
        context.dataStore.edit { it[Keys.MDNS_SERVICE_TYPE] = cleaned }
    }

    suspend fun setPingIntervalSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.PING_INTERVAL_SEC] = seconds }
    }

    suspend fun setPreferResolvedIp(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREFER_RESOLVED_IP] = enabled }
    }

    suspend fun setForceMobileViewport(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FORCE_MOBILE_VIEWPORT] = enabled }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setShowPingLatencyOnCard(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PING_LATENCY] = enabled }
    }

    suspend fun setCompactGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.COMPACT_GRID] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }
}
