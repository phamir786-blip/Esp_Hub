package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.EspDevice
import com.example.network.DiscoveredMdnsNode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.TuyaOrange
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MdnsRadarScreen(
    isScanning: Boolean,
    scanStatusMessage: String,
    discoveredNodes: List<DiscoveredMdnsNode>,
    savedDevices: List<EspDevice>,
    autoAddEnabled: Boolean,
    serviceType: String,
    onToggleScan: () -> Unit,
    onToggleAutoAdd: (Boolean) -> Unit,
    onAddNode: (DiscoveredMdnsNode) -> Unit,
    onOpenManualAdd: () -> Unit,
    onOpenSavedDevice: (EspDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "mDNS Auto-Discovery",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Scans home Wi-Fi for ESP32 ($serviceType) with zero server needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onOpenManualAdd,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("radar_manual_add_button")
                ) {
                    Icon(Icons.Filled.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual .local")
                }
            }
        }

        // Animated Tuya-Style mDNS Radar Visualizer Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MdnsRadarCanvas(isScanning = isScanning, discoveredCount = discoveredNodes.size)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = scanStatusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isScanning) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleScan,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("toggle_radar_scan_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.Radar,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScanning) "Pause Radar Scan" else "Start Wi-Fi mDNS Scan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Auto-Add Toggle Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            text = "Auto-Add Discovered ESP32s",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Automatically adds new .local devices found on your home Wi-Fi to My Home",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoAddEnabled,
                        onCheckedChange = onToggleAutoAdd,
                        modifier = Modifier.testTag("radar_auto_add_switch")
                    )
                }
            }
        }

        // Discovered Nodes Header
        item {
            Text(
                text = "Discovered on Local Wi-Fi (${discoveredNodes.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (discoveredNodes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Waiting for ESP32 mDNS broadcasts…",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ensure your ESP32 calls MDNS.begin(\"name\") and MDNS.addService(\"http\", \"tcp\", 80), or tap 'Manual .local' above to add any .local hostname directly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(discoveredNodes, key = { it.mdnsHostname }) { node ->
                val matchingSaved = savedDevices.firstOrNull {
                    it.cleanMdnsHost.equals(node.mdnsHostname, ignoreCase = true)
                }
                DiscoveredNodeCard(
                    node = node,
                    savedDevice = matchingSaved,
                    onAdd = { onAddNode(node) },
                    onOpen = { if (matchingSaved != null) onOpenSavedDevice(matchingSaved) }
                )
            }
        }
    }
}

@Composable
private fun MdnsRadarCanvas(
    isScanning: Boolean,
    discoveredCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val pulseRadiusRatio by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(168.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // Concentric radar rings
            listOf(0.32f, 0.64f, 0.95f).forEach { factor ->
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.18f),
                    radius = maxRadius * factor,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            if (isScanning) {
                // Expanding pulse wave
                drawCircle(
                    color = TuyaOrange.copy(alpha = (1f - pulseRadiusRatio) * 0.45f),
                    radius = maxRadius * pulseRadiusRatio,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Rotating radar sweep line
                val rad = Math.toRadians(sweepAngle.toDouble())
                val endX = center.x + (maxRadius * 0.95f * cos(rad)).toFloat()
                val endY = center.y + (maxRadius * 0.95f * sin(rad)).toFloat()

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(TuyaOrange, ElectricCyan),
                        start = center,
                        end = Offset(endX, endY)
                    ),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Center hub node
            drawCircle(
                color = if (isScanning) TuyaOrange else ElectricCyan,
                radius = 12.dp.toPx(),
                center = center
            )

            // Draw blips for discovered nodes
            for (i in 0 until discoveredCount.coerceAtMost(6)) {
                val angleRad = Math.toRadians((i * 60 + 35).toDouble())
                val dist = maxRadius * 0.58f
                val blipCenter = Offset(
                    x = center.x + (dist * cos(angleRad)).toFloat(),
                    y = center.y + (dist * sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = OnlineEmerald,
                    radius = 6.dp.toPx(),
                    center = blipCenter
                )
            }
        }
    }
}

@Composable
private fun DiscoveredNodeCard(
    node: DiscoveredMdnsNode,
    savedDevice: EspDevice?,
    onAdd: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = node.inferredCategory.accentColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = node.inferredCategory.icon,
                            contentDescription = null,
                            tint = node.inferredCategory.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.serviceName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${node.mdnsHostname} → ${node.hostIp}:${node.port}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (savedDevice != null) {
                FilledTonalButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = OnlineEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open App")
                }
            } else {
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}
