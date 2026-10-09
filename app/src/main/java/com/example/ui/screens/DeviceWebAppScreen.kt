package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EspDevice
import com.example.data.preferences.HubSettings
import com.example.ui.components.TuyaCategorizedControlPanel
import com.example.ui.theme.OnlineEmerald
import com.example.ui.webview.EspWebDomBridge

enum class PresentationMode(val label: String) {
    TUYA_CATEGORIZED("Tuya Smart UI"),
    STYLED_WEB("Styled Web"),
    RAW_WEB("Raw Web")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DeviceWebAppScreen(
    device: EspDevice,
    settings: HubSettings,
    onBack: () -> Unit,
    onRetryPing: (EspDevice) -> Unit
) {
    val localView = LocalView.current
    DisposableEffect(settings.keepScreenAwake) {
        val prev = localView.keepScreenOn
        localView.keepScreenOn = settings.keepScreenAwake
        onDispose {
            localView.keepScreenOn = prev
        }
    }

    val domBridge = remember(device.id) { EspWebDomBridge() }
    val parsedSchema by domBridge.schema.collectAsStateWithLifecycle()

    var presentationMode by remember { mutableStateOf(PresentationMode.TUYA_CATEGORIZED) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(15) }
    var hasLoadError by remember { mutableStateOf(false) }
    var isSimulatedPreview by remember { mutableStateOf(false) }
    var errorDescription by remember { mutableStateOf<String?>(null) }

    var useResolvedIpMode by remember(device.id, settings.preferResolvedIp) {
        mutableStateOf(settings.preferResolvedIp && !device.lastResolvedIp.isNullOrBlank())
    }

    // Support OTA Firmware file uploads (<input type="file">) inside ESP32 Web UI
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fileUploadCallback?.onReceiveValue(arrayOf(uri))
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    val activeTargetUrl = remember(device, useResolvedIpMode) {
        device.buildTargetUrl(preferResolvedIp = useResolvedIpMode)
    }

    BackHandler {
        val wv = webViewRef
        if (presentationMode != PresentationMode.TUYA_CATEGORIZED && wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Native Tuya Top Bar + Presentation Mode Switcher
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("webview_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Hub"
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = device.category.accentColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = device.category.icon,
                                        contentDescription = null,
                                        tint = device.category.accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (!hasLoadError || isSimulatedPreview) OnlineEmerald
                                                else MaterialTheme.colorScheme.error
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isSimulatedPreview) {
                                            "${device.cleanMdnsHost} • Tuya Categorized Preview"
                                        } else if (useResolvedIpMode && !device.lastResolvedIp.isNullOrBlank()) {
                                            "${device.cleanMdnsHost} (${device.lastResolvedIp})"
                                        } else {
                                            device.cleanMdnsHost
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!device.lastResolvedIp.isNullOrBlank()) {
                                IconButton(
                                    onClick = {
                                        useResolvedIpMode = !useResolvedIpMode
                                        hasLoadError = false
                                        isSimulatedPreview = false
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_address_mode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Dns,
                                        contentDescription = "Switch mDNS / Resolved IP mode",
                                        tint = if (useResolvedIpMode) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (isSimulatedPreview) {
                                        webViewRef?.evaluateJavascript("window.__tuyaScanDom && window.__tuyaScanDom();", null)
                                    } else {
                                        hasLoadError = false
                                        onRetryPing(device)
                                        webViewRef?.reload()
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("webview_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Sync & Rescan ESP32 Web-App"
                                )
                            }
                        }
                    }

                    // Segmented Switcher: "Tuya Smart UI" (Categorized) vs "Styled Web" vs "Raw Web"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresentationMode.entries.forEach { mode ->
                            val selected = presentationMode == mode
                            Surface(
                                onClick = {
                                    presentationMode = mode
                                    if (mode == PresentationMode.STYLED_WEB) {
                                        webViewRef?.evaluateJavascript(EspWebDomBridge.TUYA_WEB_STYLE_CSS_JS, null)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) {
                                    device.category.accentColor.copy(alpha = 0.20f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) device.category.accentColor else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_tab_${mode.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (mode == PresentationMode.TUYA_CATEGORIZED) {
                                            Icons.Filled.AutoAwesomeMosaic
                                        } else {
                                            Icons.Filled.Web
                                        },
                                        contentDescription = null,
                                        tint = if (selected) device.category.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) device.category.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    progress = { (loadProgress / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = device.category.accentColor,
                    trackColor = Color.Transparent
                )
            }

            // Main Body: Background WebView + Categorized Tuya Native UI Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Live WebView Engine (Visible when in STYLED_WEB / RAW_WEB, or running silently in background when in TUYA_CATEGORIZED)
                val showDirectWebView = presentationMode != PresentationMode.TUYA_CATEGORIZED && !hasLoadError
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (showDirectWebView) 1f else 0f)
                        .testTag("esp_device_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER

                            // Register Two-Way Tuya DOM Bridge
                            addJavascriptInterface(domBridge, "AndroidTuyaBridge")

                            this.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                setSupportZoom(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mediaPlaybackRequiresUserGesture = false
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    if (newProgress >= 95) {
                                        isLoading = false
                                    }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    fileUploadCallback?.onReceiveValue(null)
                                    fileUploadCallback = filePathCallback
                                    filePickerLauncher.launch("*/*")
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    // Inject the Tuya DOM Scanner & Categorizer
                                    view?.evaluateJavascript(EspWebDomBridge.DOM_EXTRACTOR_JS, null)
                                    if (presentationMode == PresentationMode.STYLED_WEB) {
                                        view?.evaluateJavascript(EspWebDomBridge.TUYA_WEB_STYLE_CSS_JS, null)
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true && !isSimulatedPreview) {
                                        isLoading = false
                                        hasLoadError = true
                                        errorDescription = error?.description?.toString()
                                            ?: "ESP32 node unreachable on local Wi-Fi"
                                    }
                                }
                            }

                            webViewRef = this
                            loadUrl(activeTargetUrl)
                        }
                    }
                )

                // Primary View: Categorized Tuya Native Control UI
                if (presentationMode == PresentationMode.TUYA_CATEGORIZED && (!hasLoadError || isSimulatedPreview)) {
                    TuyaCategorizedControlPanel(
                        device = device,
                        schema = parsedSchema,
                        isSimulatedPreview = isSimulatedPreview,
                        onTriggerToggle = { toggle, checked ->
                            webViewRef?.evaluateJavascript(
                                "window.__tuyaTriggerElement('${toggle.id}', $checked);",
                                null
                            )
                        },
                        onTriggerSlider = { slider, value ->
                            webViewRef?.evaluateJavascript(
                                "window.__tuyaTriggerElement('${slider.id}', $value);",
                                null
                            )
                        },
                        onTriggerColor = { colorPicker, hex ->
                            webViewRef?.evaluateJavascript(
                                "window.__tuyaTriggerElement('${colorPicker.id}', '$hex');",
                                null
                            )
                        },
                        onTriggerMode = { modeSelect, optionValue ->
                            webViewRef?.evaluateJavascript(
                                "window.__tuyaTriggerElement('${modeSelect.id}', '$optionValue');",
                                null
                            )
                        },
                        onTriggerAction = { action ->
                            webViewRef?.evaluateJavascript(
                                "window.__tuyaTriggerElement('${action.id}', null);",
                                null
                            )
                        },
                        onSwitchToRawWeb = {
                            presentationMode = PresentationMode.STYLED_WEB
                            webViewRef?.evaluateJavascript(EspWebDomBridge.TUYA_WEB_STYLE_CSS_JS, null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Native Offline / Unreachable Overlay (with 1-tap Preview of Categorized Tuya UI!)
                androidx.compose.animation.AnimatedVisibility(
                    visible = hasLoadError && !isSimulatedPreview,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NativeOfflineConnectionView(
                        device = device,
                        attemptedUrl = activeTargetUrl,
                        errorDetail = errorDescription,
                        useResolvedIpMode = useResolvedIpMode,
                        onRetry = {
                            hasLoadError = false
                            isSimulatedPreview = false
                            isLoading = true
                            onRetryPing(device)
                            webViewRef?.loadUrl(activeTargetUrl)
                        },
                        onPreviewTuyaCategorizedUi = {
                            hasLoadError = false
                            isSimulatedPreview = true
                            isLoading = false
                            presentationMode = PresentationMode.TUYA_CATEGORIZED
                            val demoHtml = EspWebDomBridge.buildInteractiveEsp32WebPage(device)
                            webViewRef?.loadDataWithBaseURL(
                                activeTargetUrl,
                                demoHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        onToggleAddressMode = {
                            useResolvedIpMode = !useResolvedIpMode
                            hasLoadError = false
                            isSimulatedPreview = false
                            isLoading = true
                            val nextUrl = device.buildTargetUrl(preferResolvedIp = useResolvedIpMode)
                            webViewRef?.loadUrl(nextUrl)
                        },
                        onBackToHub = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeOfflineConnectionView(
    device: EspDevice,
    attemptedUrl: String,
    errorDetail: String?,
    useResolvedIpMode: Boolean,
    onRetry: () -> Unit,
    onPreviewTuyaCategorizedUi: () -> Unit,
    onToggleAddressMode: () -> Unit,
    onBackToHub: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = device.category.accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, device.category.accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = device.category.accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "${device.name} Not Found on Wi-Fi",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Could not reach ${device.cleanMdnsHost}. You can retry mDNS resolution or preview how this device's web-app is categorized into the Tuya Smart UI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Target: $attemptedUrl",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!errorDetail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = errorDetail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onPreviewTuyaCategorizedUi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("preview_tuya_ui_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.AutoAwesomeMosaic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Tuya Categorized Control UI", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retry_webview_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry mDNS Connection (${device.cleanMdnsHost})")
            }

            if (!device.lastResolvedIp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onToggleAddressMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Dns, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (useResolvedIpMode) {
                            "Try Direct .local Hostname"
                        } else {
                            "Try Last Resolved IP (${device.lastResolvedIp})"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBackToHub,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Smart Hub")
            }
        }
    }
}
