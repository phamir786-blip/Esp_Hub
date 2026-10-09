package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CardBackgroundPreset
import com.example.data.model.DeviceCategory
import com.example.data.model.EspDevice

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditDeviceSheet(
    editingDevice: EspDevice?,
    availableRooms: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        editingId: Long?,
        name: String,
        mdnsHost: String,
        port: Int,
        webPath: String,
        roomName: String,
        category: DeviceCategory,
        bgPreset: CardBackgroundPreset,
        customBgUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editingDevice) { mutableStateOf(editingDevice?.name ?: "") }
    var mdnsHost by remember(editingDevice) { mutableStateOf(editingDevice?.mdnsHostname ?: "") }
    var portText by remember(editingDevice) { mutableStateOf((editingDevice?.port ?: 80).toString()) }
    var webPath by remember(editingDevice) { mutableStateOf(editingDevice?.webPath ?: "/") }
    var selectedRoom by remember(editingDevice) {
        mutableStateOf(editingDevice?.roomName ?: "Living Room")
    }
    var selectedCategory by remember(editingDevice) {
        mutableStateOf(editingDevice?.category ?: DeviceCategory.LIGHT)
    }
    var selectedBgPreset by remember(editingDevice) {
        mutableStateOf(editingDevice?.bgPreset ?: CardBackgroundPreset.TUYA_EMBER)
    }
    var customBgUri by remember(editingDevice) {
        mutableStateOf(editingDevice?.customBgUri)
    }
    var hostError by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for custom card background
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            customBgUri = uri.toString()
            selectedBgPreset = CardBackgroundPreset.CUSTOM_IMAGE
        }
    }

    val normalizedPreviewHost = remember(mdnsHost) {
        val raw = mdnsHost.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .lowercase()
        when {
            raw.isEmpty() -> "esp32-device.local"
            raw.endsWith(".local") || raw.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> raw
            else -> "$raw.local"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (editingDevice == null) "Add ESP32 via mDNS" else "Customize ${editingDevice.name}",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Uses .local mDNS hostname so your hub never loses the device even when router DHCP changes its IP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = mdnsHost,
                onValueChange = {
                    mdnsHost = it
                    hostError = false
                    if (editingDevice == null && name.isBlank()) {
                        selectedCategory = DeviceCategory.inferFromName(it)
                        selectedBgPreset = selectedCategory.defaultBgPreset
                    }
                },
                label = { Text("mDNS Address (e.g. wled-lamp.local)") },
                placeholder = { Text("livingroom-esp32.local") },
                leadingIcon = {
                    Icon(Icons.Filled.Dns, contentDescription = null)
                },
                isError = hostError,
                supportingText = {
                    Text("Target URL: http://$normalizedPreviewHost:${portText.ifBlank { "80" }}${webPath.ifBlank { "/" }}")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_mdns_hostname")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Device Display Name") },
                placeholder = { Text("Living Room Ambient Light") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_device_name")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.38f)
                )
                OutlinedTextField(
                    value = webPath,
                    onValueChange = { webPath = it },
                    label = { Text("Web Path") },
                    placeholder = { Text("/") },
                    singleLine = true,
                    modifier = Modifier.weight(0.62f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Device Icon / Category Selection
            Text(
                text = "Device Icon (Light / Music / Climate / etc.)",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(DeviceCategory.entries) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            category.accentColor.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) category.accentColor else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedCategory = category
                                if (selectedBgPreset != CardBackgroundPreset.CUSTOM_IMAGE) {
                                    selectedBgPreset = category.defaultBgPreset
                                }
                            }
                            .testTag("category_chip_${category.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
                                tint = category.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Custom Card Background Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card Background",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("pick_custom_bg_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (customBgUri != null && selectedBgPreset == CardBackgroundPreset.CUSTOM_IMAGE) {
                            "Change Photo"
                        } else {
                            "Custom Gallery Photo"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(CardBackgroundPreset.entries.filter { it != CardBackgroundPreset.CUSTOM_IMAGE }) { preset ->
                    val isSelected = selectedBgPreset == preset
                    Box(
                        modifier = Modifier
                            .size(width = 104.dp, height = 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(preset.startColor, preset.endColor)
                                )
                            )
                            .clickable { selectedBgPreset = preset }
                            .padding(10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(preset.glowColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Room Selection
            Text(
                text = "Room / Zone",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(6.dp))

            val roomOptions = remember(availableRooms) {
                (availableRooms.filter { it != "All Devices" } +
                    listOf("Living Room", "Bedroom", "Studio", "Workshop", "Kitchen", "Outdoor"))
                    .distinct()
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                roomOptions.forEach { room ->
                    FilterChip(
                        selected = selectedRoom.equals(room, ignoreCase = true),
                        onClick = { selectedRoom = room },
                        label = { Text(room) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (mdnsHost.isBlank()) {
                            hostError = true
                            return@Button
                        }
                        val parsedPort = portText.toIntOrNull() ?: 80
                        val finalDisplayName = name.trim().ifEmpty {
                            normalizedPreviewHost.removeSuffix(".local")
                                .replace('-', ' ')
                                .replaceFirstChar { it.uppercase() }
                        }
                        onSave(
                            editingDevice?.id,
                            finalDisplayName,
                            normalizedPreviewHost,
                            parsedPort,
                            webPath,
                            selectedRoom,
                            selectedCategory,
                            selectedBgPreset,
                            if (selectedBgPreset == CardBackgroundPreset.CUSTOM_IMAGE) customBgUri else null
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_device_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (editingDevice == null) "Save to Hub" else "Update Device",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
