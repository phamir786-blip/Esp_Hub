package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EspDevice
import com.example.ui.theme.OnlineEmerald
import com.example.ui.webview.BridgedAction
import com.example.ui.webview.BridgedColorPicker
import com.example.ui.webview.BridgedSelectMode
import com.example.ui.webview.BridgedSlider
import com.example.ui.webview.BridgedTelemetry
import com.example.ui.webview.BridgedToggle
import com.example.ui.webview.EspParsedWebSchema
import kotlin.math.roundToInt

enum class TuyaControlCategoryTab(val title: String) {
    ALL("All Controls"),
    POWER("Switches"),
    SLIDERS("Sliders & Levels"),
    MODES("Modes & Color"),
    TELEMETRY("Telemetry"),
    ACTIONS("Actions")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuyaCategorizedControlPanel(
    device: EspDevice,
    schema: EspParsedWebSchema,
    isSimulatedPreview: Boolean,
    onTriggerToggle: (BridgedToggle, Boolean) -> Unit,
    onTriggerSlider: (BridgedSlider, Float) -> Unit,
    onTriggerColor: (BridgedColorPicker, String) -> Unit,
    onTriggerMode: (BridgedSelectMode, String) -> Unit,
    onTriggerAction: (BridgedAction) -> Unit,
    onSwitchToRawWeb: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryTab by remember { mutableStateOf(TuyaControlCategoryTab.ALL) }
    val accent = device.category.accentColor

    val primaryToggle = schema.toggles.firstOrNull()
    val primarySlider = schema.sliders.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tuya_categorized_panel"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tuya Hero Centerpiece Card (Master Power Ring + Primary Level Readout)
        item {
            TuyaHeroDeviceCenterpiece(
                device = device,
                primaryToggle = primaryToggle,
                primarySlider = primarySlider,
                isSimulatedPreview = isSimulatedPreview,
                onTogglePrimary = { toggle ->
                    onTriggerToggle(toggle, !toggle.isChecked)
                }
            )
        }

        // 2. Category Filter Pills (All Controls, Switches, Sliders, Modes & Color, Telemetry, Actions)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(TuyaControlCategoryTab.entries) { tab ->
                    val isSelected = selectedCategoryTab == tab
                    Surface(
                        onClick = { selectedCategoryTab = tab },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) accent else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.testTag("tuya_cat_tab_${tab.name}")
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Category: Power & Switches
        if ((selectedCategoryTab == TuyaControlCategoryTab.ALL || selectedCategoryTab == TuyaControlCategoryTab.POWER) &&
            schema.toggles.isNotEmpty()
        ) {
            item {
                TuyaCategorySectionCard(
                    title = "Power & Switches",
                    subtitle = "${schema.toggles.size} toggle controls mapped from web-app",
                    icon = Icons.Filled.PowerSettingsNew,
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        schema.toggles.forEach { toggle ->
                            TuyaToggleControlRow(
                                toggle = toggle,
                                accentColor = accent,
                                onToggle = { checked -> onTriggerToggle(toggle, checked) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Category: Sliders & Levels (Brightness, Volume, Speed, Temperature, PWM)
        if ((selectedCategoryTab == TuyaControlCategoryTab.ALL || selectedCategoryTab == TuyaControlCategoryTab.SLIDERS) &&
            schema.sliders.isNotEmpty()
        ) {
            item {
                TuyaCategorySectionCard(
                    title = "Levels & Dimming Sliders",
                    subtitle = "Real-time analog/PWM control synced with ESP32",
                    icon = Icons.Filled.Tune,
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        schema.sliders.forEach { slider ->
                            TuyaSliderControlCard(
                                slider = slider,
                                accentColor = accent,
                                onValueCommit = { newVal -> onTriggerSlider(slider, newVal) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Category: Modes, Scenes & RGB Color Pickers
        if ((selectedCategoryTab == TuyaControlCategoryTab.ALL || selectedCategoryTab == TuyaControlCategoryTab.MODES) &&
            (schema.colors.isNotEmpty() || schema.modes.isNotEmpty())
        ) {
            item {
                TuyaCategorySectionCard(
                    title = "Scenes, Modes & Color",
                    subtitle = "Select operating presets and ambient colors",
                    icon = Icons.Filled.ColorLens,
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        schema.colors.forEach { colorPicker ->
                            TuyaColorSwatchSelector(
                                colorPicker = colorPicker,
                                onPickHex = { hex -> onTriggerColor(colorPicker, hex) }
                            )
                        }

                        schema.modes.forEach { modeSelect ->
                            Column {
                                Text(
                                    text = modeSelect.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    modeSelect.options.forEach { opt ->
                                        val isCurrent = modeSelect.selectedValue == opt.value
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = { onTriggerMode(modeSelect, opt.value) },
                                            label = { Text(opt.text) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accent.copy(alpha = 0.22f),
                                                selectedLabelColor = accent
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isCurrent,
                                                selectedBorderColor = accent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Category: Live Telemetry & Status Readouts
        if ((selectedCategoryTab == TuyaControlCategoryTab.ALL || selectedCategoryTab == TuyaControlCategoryTab.TELEMETRY) &&
            schema.telemetry.isNotEmpty()
        ) {
            item {
                TuyaCategorySectionCard(
                    title = "Live Sensor & Node Telemetry",
                    subtitle = "Real-time status values extracted from ESP32",
                    icon = Icons.Filled.Sensors,
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        schema.telemetry.chunked(2).forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPair.forEach { metric ->
                                    TuyaTelemetryTile(
                                        metric = metric,
                                        accentColor = accent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Category: Quick Commands & Actions
        if ((selectedCategoryTab == TuyaControlCategoryTab.ALL || selectedCategoryTab == TuyaControlCategoryTab.ACTIONS) &&
            schema.actions.isNotEmpty()
        ) {
            item {
                TuyaCategorySectionCard(
                    title = "Quick Commands & Presets",
                    subtitle = "Instant single-tap triggers sent to ESP32 web-app",
                    icon = Icons.Filled.TouchApp,
                    accentColor = accent
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        schema.actions.forEach { action ->
                            FilledTonalButton(
                                onClick = { onTriggerAction(action) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fallback if an ESP32 page uses a custom canvas-only UI with zero standard HTML elements
        if (!schema.hasParsedContent) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanning ESP32 Web Elements…",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "If this ESP32 uses a custom canvas-only interface, you can switch to the Styled Web view at the top anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(onClick = onSwitchToRawWeb) {
                            Text("Switch to Web View")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TuyaHeroDeviceCenterpiece(
    device: EspDevice,
    primaryToggle: BridgedToggle?,
    primarySlider: BridgedSlider?,
    isSimulatedPreview: Boolean,
    onTogglePrimary: (BridgedToggle) -> Unit
) {
    val category = device.category
    val isPoweredOn = primaryToggle?.isChecked ?: true
    val accent = category.accentColor

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isPoweredOn) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isPoweredOn) accent.copy(alpha = 0.28f) else Color.Transparent,
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.35f),
                            radius = size.width * 0.65f
                        )
                    )
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isPoweredOn) OnlineEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isSimulatedPreview) {
                                    "TUYA SMART PARSER • DEMO NODE"
                                } else if (isPoweredOn) {
                                    "ACTIVE • ${device.roomName.uppercase()}"
                                } else {
                                    "STANDBY • ${device.roomName.uppercase()}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPoweredOn) OnlineEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )

                    Text(
                        text = if (primarySlider != null) {
                            "${primarySlider.value.roundToInt()}${primarySlider.unit} ${primarySlider.label}"
                        } else if (primaryToggle != null) {
                            "${primaryToggle.label}: ${if (isPoweredOn) "ON" else "OFF"}"
                        } else {
                            device.cleanMdnsHost
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Categorized Tuya UI • Connected via ${device.cleanMdnsHost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Interactive Tuya Glowing Power / Master Button Ring
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPoweredOn) {
                                Brush.linearGradient(
                                    colors = listOf(accent, accent.copy(alpha = 0.65f))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        )
                        .border(
                            width = 3.dp,
                            color = if (isPoweredOn) Color.White.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable(enabled = primaryToggle != null) {
                            if (primaryToggle != null) onTogglePrimary(primaryToggle)
                        }
                        .testTag("tuya_hero_power_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (primaryToggle != null) Icons.Filled.PowerSettingsNew else category.icon,
                        contentDescription = "Master Power Toggle",
                        tint = if (isPoweredOn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TuyaCategorySectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun TuyaToggleControlRow(
    toggle: BridgedToggle,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (toggle.isChecked) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(200),
        label = "toggle_bg"
    )

    Surface(
        onClick = { onToggle(!toggle.isChecked) },
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(
            width = 1.dp,
            color = if (toggle.isChecked) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toggle.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (toggle.isChecked) "State: ON" else "State: OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (toggle.isChecked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = toggle.isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accentColor
                )
            )
        }
    }
}

@Composable
private fun TuyaSliderControlCard(
    slider: BridgedSlider,
    accentColor: Color,
    onValueCommit: (Float) -> Unit
) {
    var localValue by remember(slider.id, slider.value) { mutableFloatStateOf(slider.value) }
    val safeMin = slider.min
    val safeMax = if (slider.max > slider.min) slider.max else slider.min + 100f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slider.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${localValue.roundToInt()}${slider.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Slider(
                value = localValue.coerceIn(safeMin, safeMax),
                onValueChange = { newVal ->
                    localValue = newVal
                    onValueCommit(newVal)
                },
                valueRange = safeMin..safeMax,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TuyaColorSwatchSelector(
    colorPicker: BridgedColorPicker,
    onPickHex: (String) -> Unit
) {
    val swatches = listOf(
        "#FF5A1F" to Color(0xFFFF5A1F),
        "#FFB300" to Color(0xFFFFB300),
        "#00E676" to Color(0xFF00E676),
        "#00E5FF" to Color(0xFF00E5FF),
        "#2979FF" to Color(0xFF2979FF),
        "#D500F9" to Color(0xFFD500F9),
        "#FF4081" to Color(0xFFFF4081),
        "#FFF8E7" to Color(0xFFFFF8E7)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorPicker.label,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = colorPicker.hexColor.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(swatches) { (hex, composeColor) ->
                val isSelected = colorPicker.hexColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(composeColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .clickable { onPickHex(hex) }
                )
            }
        }
    }
}

@Composable
private fun TuyaTelemetryTile(
    metric: BridgedTelemetry,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
