package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CardBackgroundPreset
import com.example.data.model.EspDevice
import com.example.ui.theme.OfflineSlate
import com.example.ui.theme.OnlineEmerald

@Composable
fun EspDeviceCard(
    device: EspDevice,
    showPingLatency: Boolean,
    compactMode: Boolean,
    onOpenWebApp: (EspDevice) -> Unit,
    onEditDevice: (EspDevice) -> Unit,
    onPingDevice: (EspDevice) -> Unit,
    onDeleteDevice: (EspDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val category = device.category
    val bgPreset = device.bgPreset
    val hasCustomPhoto = bgPreset == CardBackgroundPreset.CUSTOM_IMAGE && !device.customBgUri.isNullOrBlank()

    val borderColor by animateColorAsState(
        targetValue = if (device.isOnline) {
            category.accentColor.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        },
        animationSpec = tween(250),
        label = "card_border"
    )

    val cardHeight = if (compactMode) 152.dp else 184.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = cardHeight)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenWebApp(device) }
            .testTag("device_card_${device.id}"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val brush = Brush.linearGradient(
                        colors = listOf(bgPreset.startColor, bgPreset.endColor),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = brush)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bgPreset.glowColor.copy(alpha = if (device.isOnline) 0.28f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.18f),
                            radius = size.width * 0.65f
                        )
                    )
                }
        ) {
            if (hasCustomPhoto) {
                AsyncImage(
                    model = Uri.parse(device.customBgUri),
                    contentDescription = "${device.name} custom background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark glass readability scrim over custom photo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Category Icon + Online Status & Menu
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compactStatus = maxWidth < 160.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = category.accentColor.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, category.accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
                                tint = category.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadgePill(
                            isOnline = device.isOnline,
                            pingMs = if (showPingLatency) device.lastPingMs else null,
                            compact = compactStatus
                        )

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .testTag("device_menu_${device.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Options for ${device.name}",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Card & Background") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onEditDevice(device)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Resolve mDNS & Ping") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Refresh, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onPingDevice(device)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Remove Device",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteDevice(device)
                                    }
                                )
                            }
                        }
                    }
                    }
                }

                // Bottom Section: Room tag, Name, and .local mDNS address pill
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.roomName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = category.accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (device.isAutoDiscovered) {
                            Text(
                                text = "• AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = device.cleanMdnsHost,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (!device.lastResolvedIp.isNullOrBlank() && !compactMode) {
                                Text(
                                    text = " → ${device.lastResolvedIp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                    maxLines = 1
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
private fun StatusBadgePill(
    isOnline: Boolean,
    pingMs: Int?,
    compact: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "online_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val dotColor = if (isOnline) OnlineEmerald else OfflineSlate
    val statusText = when {
        compact && isOnline -> "On"
        compact -> "Off"
        isOnline && pingMs != null -> "${pingMs}ms"
        isOnline -> "Online"
        else -> "Offline"
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.50f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOnline) OnlineEmerald.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isOnline) pulseAlpha else 0.7f))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) OnlineEmerald else Color.White.copy(alpha = 0.65f)
            )
        }
    }
}
