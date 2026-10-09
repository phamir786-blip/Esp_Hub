package com.example.ui.webview

import android.webkit.JavascriptInterface
import com.example.data.model.DeviceCategory
import com.example.data.model.EspDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class BridgedToggle(
    val id: String,
    val label: String,
    val isChecked: Boolean,
    val kind: String // "checkbox" or "button"
)

data class BridgedSlider(
    val id: String,
    val label: String,
    val value: Float,
    val min: Float,
    val max: Float,
    val step: Float,
    val unit: String
)

data class BridgedColorPicker(
    val id: String,
    val label: String,
    val hexColor: String
)

data class BridgedSelectOption(
    val value: String,
    val text: String
)

data class BridgedSelectMode(
    val id: String,
    val label: String,
    val selectedValue: String,
    val options: List<BridgedSelectOption>
)

data class BridgedTelemetry(
    val label: String,
    val value: String
)

data class BridgedAction(
    val id: String,
    val label: String
)

data class EspParsedWebSchema(
    val pageTitle: String = "",
    val toggles: List<BridgedToggle> = emptyList(),
    val sliders: List<BridgedSlider> = emptyList(),
    val colors: List<BridgedColorPicker> = emptyList(),
    val modes: List<BridgedSelectMode> = emptyList(),
    val telemetry: List<BridgedTelemetry> = emptyList(),
    val actions: List<BridgedAction> = emptyList(),
    val hasParsedContent: Boolean = false,
    val lastUpdatedMs: Long = 0L
)

class EspWebDomBridge {
    private val _schema = MutableStateFlow(EspParsedWebSchema())
    val schema: StateFlow<EspParsedWebSchema> = _schema.asStateFlow()

    @JavascriptInterface
    fun onDomExtracted(jsonPayload: String) {
        try {
            val root = JSONObject(jsonPayload)
            val title = root.optString("title", "")

            val togglesList = mutableListOf<BridgedToggle>()
            val togglesArr = root.optJSONArray("toggles")
            if (togglesArr != null) {
                for (i in 0 until togglesArr.length()) {
                    val obj = togglesArr.getJSONObject(i)
                    togglesList.add(
                        BridgedToggle(
                            id = obj.optString("id"),
                            label = obj.optString("label", "Switch ${i + 1}"),
                            isChecked = obj.optBoolean("checked", false),
                            kind = obj.optString("kind", "checkbox")
                        )
                    )
                }
            }

            val slidersList = mutableListOf<BridgedSlider>()
            val slidersArr = root.optJSONArray("sliders")
            if (slidersArr != null) {
                for (i in 0 until slidersArr.length()) {
                    val obj = slidersArr.getJSONObject(i)
                    slidersList.add(
                        BridgedSlider(
                            id = obj.optString("id"),
                            label = obj.optString("label", "Level ${i + 1}"),
                            value = obj.optDouble("value", 50.0).toFloat(),
                            min = obj.optDouble("min", 0.0).toFloat(),
                            max = obj.optDouble("max", 100.0).toFloat(),
                            step = obj.optDouble("step", 1.0).toFloat().coerceAtLeast(0.1f),
                            unit = obj.optString("unit", "%")
                        )
                    )
                }
            }

            val colorsList = mutableListOf<BridgedColorPicker>()
            val colorsArr = root.optJSONArray("colors")
            if (colorsArr != null) {
                for (i in 0 until colorsArr.length()) {
                    val obj = colorsArr.getJSONObject(i)
                    colorsList.add(
                        BridgedColorPicker(
                            id = obj.optString("id"),
                            label = obj.optString("label", "Color"),
                            hexColor = obj.optString("value", "#FF5A1F")
                        )
                    )
                }
            }

            val modesList = mutableListOf<BridgedSelectMode>()
            val modesArr = root.optJSONArray("modes")
            if (modesArr != null) {
                for (i in 0 until modesArr.length()) {
                    val obj = modesArr.getJSONObject(i)
                    val optsArr = obj.optJSONArray("options")
                    val opts = mutableListOf<BridgedSelectOption>()
                    if (optsArr != null) {
                        for (j in 0 until optsArr.length()) {
                            val o = optsArr.getJSONObject(j)
                            opts.add(
                                BridgedSelectOption(
                                    value = o.optString("value"),
                                    text = o.optString("text")
                                )
                            )
                        }
                    }
                    if (opts.isNotEmpty()) {
                        modesList.add(
                            BridgedSelectMode(
                                id = obj.optString("id"),
                                label = obj.optString("label", "Mode"),
                                selectedValue = obj.optString("selectedValue"),
                                options = opts
                            )
                        )
                    }
                }
            }

            val telemetryList = mutableListOf<BridgedTelemetry>()
            val telemetryArr = root.optJSONArray("telemetry")
            if (telemetryArr != null) {
                for (i in 0 until telemetryArr.length()) {
                    val obj = telemetryArr.getJSONObject(i)
                    val lbl = obj.optString("label").trim()
                    val v = obj.optString("value").trim()
                    if (lbl.isNotEmpty() && v.isNotEmpty()) {
                        telemetryList.add(BridgedTelemetry(label = lbl, value = v))
                    }
                }
            }

            val actionsList = mutableListOf<BridgedAction>()
            val actionsArr = root.optJSONArray("actions")
            if (actionsArr != null) {
                for (i in 0 until actionsArr.length()) {
                    val obj = actionsArr.getJSONObject(i)
                    actionsList.add(
                        BridgedAction(
                            id = obj.optString("id"),
                            label = obj.optString("label", "Action")
                        )
                    )
                }
            }

            val hasAny = togglesList.isNotEmpty() || slidersList.isNotEmpty() ||
                colorsList.isNotEmpty() || modesList.isNotEmpty() ||
                telemetryList.isNotEmpty() || actionsList.isNotEmpty()

            _schema.value = EspParsedWebSchema(
                pageTitle = title,
                toggles = togglesList,
                sliders = slidersList,
                colors = colorsList,
                modes = modesList,
                telemetry = telemetryList,
                actions = actionsList,
                hasParsedContent = hasAny,
                lastUpdatedMs = System.currentTimeMillis()
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        /**
         * JavaScript scanner injected into the ESP32 WebView.
         * It scans checkboxes, toggle buttons, range sliders, color inputs, dropdowns,
         * telemetry metrics, and action buttons, assigns stable bridge IDs, and sends
         * categorized JSON back to Android so we can render a 100% Tuya-style categorized UI.
         */
        val DOM_EXTRACTOR_JS = """
            (function() {
                if (window.__tuyaBridgeInstalled) {
                    window.__tuyaScanDom();
                    return;
                }
                window.__tuyaBridgeInstalled = true;
                var idCounter = 1;

                function ensureBridgeId(el) {
                    if (!el.getAttribute('data-tuya-id')) {
                        el.setAttribute('data-tuya-id', 'tuya_el_' + (idCounter++));
                    }
                    return el.getAttribute('data-tuya-id');
                }

                function findLabelFor(el, fallback) {
                    if (el.getAttribute('aria-label')) return el.getAttribute('aria-label').trim();
                    if (el.getAttribute('data-label')) return el.getAttribute('data-label').trim();
                    if (el.id) {
                        var lbl = document.querySelector('label[for="' + el.id + '"]');
                        if (lbl && lbl.innerText.trim()) return lbl.innerText.trim();
                    }
                    var parentLabel = el.closest('label');
                    if (parentLabel && parentLabel.innerText.trim()) {
                        return parentLabel.innerText.trim().split('\n')[0].trim();
                    }
                    var prev = el.previousElementSibling;
                    if (prev && prev.innerText && prev.innerText.trim().length < 40) {
                        return prev.innerText.trim().replace(/:$/, '');
                    }
                    var parent = el.parentElement;
                    if (parent) {
                        var clone = parent.cloneNode(true);
                        var inputs = clone.querySelectorAll('input, select, button, script, style');
                        for (var i = 0; i < inputs.length; i++) inputs[i].remove();
                        var txt = (clone.innerText || '').trim().split('\n')[0].trim().replace(/:$/, '');
                        if (txt && txt.length > 1 && txt.length < 42) return txt;
                    }
                    return el.name || el.id || fallback;
                }

                window.__tuyaTriggerElement = function(bridgeId, newValue) {
                    var el = document.querySelector('[data-tuya-id="' + bridgeId + '"]');
                    if (!el) return;
                    var tag = el.tagName.toLowerCase();
                    var type = (el.getAttribute('type') || '').toLowerCase();

                    if (type === 'checkbox') {
                        el.checked = (newValue === 'true' || newValue === true || !el.checked);
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        if (typeof el.onclick === 'function') el.onclick();
                    } else if (type === 'range' || type === 'number' || type === 'color' || tag === 'select') {
                        el.value = newValue;
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                    } else {
                        el.click();
                    }
                    setTimeout(window.__tuyaScanDom, 120);
                };

                window.__tuyaScanDom = function() {
                    var payload = {
                        title: document.title || 'ESP32 Controller',
                        toggles: [],
                        sliders: [],
                        colors: [],
                        modes: [],
                        telemetry: [],
                        actions: []
                    };

                    // 1. Checkboxes / Switches
                    var checkboxes = document.querySelectorAll('input[type="checkbox"]');
                    for (var i = 0; i < checkboxes.length; i++) {
                        var cb = checkboxes[i];
                        var id = ensureBridgeId(cb);
                        payload.toggles.push({
                            id: id,
                            label: findLabelFor(cb, 'Switch ' + (i + 1)),
                            checked: !!cb.checked,
                            kind: 'checkbox'
                        });
                    }

                    // 2. Range Sliders
                    var ranges = document.querySelectorAll('input[type="range"]');
                    for (var j = 0; j < ranges.length; j++) {
                        var rng = ranges[j];
                        var rId = ensureBridgeId(rng);
                        var rLabel = findLabelFor(rng, 'Level ' + (j + 1));
                        var unit = rng.getAttribute('data-unit') || (parseFloat(rng.max || 100) === 100 ? '%' : '');
                        payload.sliders.push({
                            id: rId,
                            label: rLabel,
                            value: parseFloat(rng.value || 0),
                            min: parseFloat(rng.min || 0),
                            max: parseFloat(rng.max || 100),
                            step: parseFloat(rng.step || 1),
                            unit: unit
                        });
                    }

                    // 3. Color Pickers
                    var colorInputs = document.querySelectorAll('input[type="color"]');
                    for (var c = 0; c < colorInputs.length; c++) {
                        var clr = colorInputs[c];
                        var cId = ensureBridgeId(clr);
                        payload.colors.push({
                            id: cId,
                            label: findLabelFor(clr, 'Color'),
                            value: clr.value || '#ff5a1f'
                        });
                    }

                    // 4. Select Dropdowns (Modes / Effects / Presets)
                    var selects = document.querySelectorAll('select');
                    for (var s = 0; s < selects.length; s++) {
                        var sel = selects[s];
                        var sId = ensureBridgeId(sel);
                        var opts = [];
                        for (var o = 0; o < sel.options.length; o++) {
                            opts.push({
                                value: sel.options[o].value,
                                text: (sel.options[o].text || sel.options[o].value).trim()
                            });
                        }
                        payload.modes.push({
                            id: sId,
                            label: findLabelFor(sel, 'Mode ' + (s + 1)),
                            selectedValue: sel.value,
                            options: opts
                        });
                    }

                    // 5. Buttons & Links (Categorized into Toggle Buttons vs Action Commands)
                    var buttons = document.querySelectorAll('button, input[type="button"], input[type="submit"], a[href]');
                    for (var b = 0; b < buttons.length; b++) {
                        var btn = buttons[b];
                        var text = (btn.innerText || btn.value || btn.getAttribute('title') || '').trim();
                        if (!text || text.length > 36) continue;
                        var bId = ensureBridgeId(btn);
                        var lower = text.toLowerCase();
                        var isActiveState = btn.classList.contains('active') ||
                            btn.classList.contains('on') ||
                            btn.getAttribute('data-state') === 'on' ||
                            btn.getAttribute('aria-pressed') === 'true';

                        if (lower.indexOf('power') !== -1 || lower.indexOf('toggle') !== -1 ||
                            lower === 'on' || lower === 'off' || lower.indexOf('relay') !== -1 ||
                            lower.indexOf('mute') !== -1 || btn.hasAttribute('data-toggle')) {
                            payload.toggles.push({
                                id: bId,
                                label: text,
                                checked: isActiveState || lower.indexOf('on') !== -1,
                                kind: 'button'
                            });
                        } else {
                            payload.actions.push({
                                id: bId,
                                label: text
                            });
                        }
                    }

                    // 6. Telemetry / Sensor Status Readings
                    var metricNodes = document.querySelectorAll('[data-telemetry], .metric, .sensor, .status-item, tr');
                    for (var m = 0; m < metricNodes.length; m++) {
                        var node = metricNodes[m];
                        if (node.tagName.toLowerCase() === 'tr') {
                            var cells = node.querySelectorAll('td, th');
                            if (cells.length === 2) {
                                var k = (cells[0].innerText || '').trim().replace(/:$/, '');
                                var v = (cells[1].innerText || '').trim();
                                if (k && v && k.length < 30 && v.length < 30) {
                                    payload.telemetry.push({ label: k, value: v });
                                }
                            }
                        } else {
                            var lAttr = node.getAttribute('data-label');
                            var vAttr = node.getAttribute('data-value') || (node.innerText || '').trim();
                            if (lAttr && vAttr) {
                                payload.telemetry.push({ label: lAttr, value: vAttr });
                            }
                        }
                    }

                    if (window.AndroidTuyaBridge && window.AndroidTuyaBridge.onDomExtracted) {
                        window.AndroidTuyaBridge.onDomExtracted(JSON.stringify(payload));
                    }
                };

                window.__tuyaScanDom();

                var observer = new MutationObserver(function() {
                    if (window.__tuyaDebounce) clearTimeout(window.__tuyaDebounce);
                    window.__tuyaDebounce = setTimeout(window.__tuyaScanDom, 180);
                });
                if (document.body) {
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        characterData: true
                    });
                }
            })();
        """.trimIndent()

        /**
         * CSS injected when user switches to "Tuya-Styled WebView" tab so even raw HTML looks sleek.
         */
        val TUYA_WEB_STYLE_CSS_JS = """
            (function() {
                if (document.getElementById('__tuya_theme_css')) return;
                var style = document.createElement('style');
                style.id = '__tuya_theme_css';
                style.innerHTML = `
                    body {
                        background-color: #070A0F !important;
                        color: #F8FAFC !important;
                        font-family: -apple-system, BlinkMacSystemFont, sans-serif !important;
                        padding: 16px !important;
                        margin: 0 !important;
                    }
                    button, input[type="button"], input[type="submit"], a {
                        background: linear-gradient(135deg, #FF5A1F, #FF8552) !important;
                        color: #FFFFFF !important;
                        border: none !important;
                        border-radius: 14px !important;
                        padding: 12px 18px !important;
                        font-weight: 600 !important;
                        margin: 6px 4px !important;
                        display: inline-block !important;
                        text-decoration: none !important;
                    }
                    input[type="range"] {
                        width: 100% !important;
                        accent-color: #FF5A1F !important;
                        height: 8px !important;
                        margin: 10px 0 !important;
                    }
                    select, input[type="text"], input[type="number"] {
                        background-color: #11161F !important;
                        color: #F8FAFC !important;
                        border: 1px solid #1E2636 !important;
                        border-radius: 12px !important;
                        padding: 10px 14px !important;
                    }
                    table {
                        width: 100% !important;
                        border-collapse: collapse !important;
                        background: #0B0F17 !important;
                        border-radius: 14px !important;
                        overflow: hidden !important;
                    }
                    td, th {
                        padding: 10px 14px !important;
                        border-bottom: 1px solid #1E2636 !important;
                    }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent()

        /**
         * Realistic interactive ESP32 Web Server HTML used when testing or previewing an unreachable
         * node, demonstrating how a raw ESP32 HTML page gets parsed and categorized into the Tuya UI.
         */
        fun buildInteractiveEsp32WebPage(device: EspDevice): String {
            return when (device.category) {
                DeviceCategory.LIGHT, DeviceCategory.LED_STRIP -> """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${device.name}</title></head>
                    <body>
                        <h2>${device.name} (${device.cleanMdnsHost})</h2>
                        <label><input type="checkbox" id="master_power" checked data-label="Master Light Power"> Master Light Power</label>
                        <label><input type="checkbox" id="night_glow" data-label="Ambient Night Mode"> Ambient Night Mode</label>
                        <label><input type="checkbox" id="music_sync" data-label="Audio Reactive Sync"> Audio Reactive Sync</label>
                        
                        <div>
                            <label for="brightness">Brightness</label>
                            <input type="range" id="brightness" min="1" max="100" value="82" data-unit="%">
                        </div>
                        <div>
                            <label for="color_temp">Warm / Cool White</label>
                            <input type="range" id="color_temp" min="2700" max="6500" step="100" value="4200" data-unit="K">
                        </div>
                        <div>
                            <label for="effect_speed">Effect Speed</label>
                            <input type="range" id="effect_speed" min="0" max="100" value="45" data-unit="%">
                        </div>
                        
                        <div>
                            <label for="led_color">Primary LED Color</label>
                            <input type="color" id="led_color" value="#FF5A1F">
                        </div>
                        
                        <div>
                            <label for="lighting_scene">Lighting Scene / Effect</label>
                            <select id="lighting_scene">
                                <option value="solid" selected>Solid Warm Glow</option>
                                <option value="aurora">Cyber Aurora Wave</option>
                                <option value="breathe">Calm Breathing</option>
                                <option value="sunset">Tuya Sunset Gradient</option>
                                <option value="party">Spectrum Pulse</option>
                            </select>
                        </div>
                        
                        <table>
                            <tr><td>ESP32 Chip</td><td>ESP32-S3 Dual Core</td></tr>
                            <tr><td>Wi-Fi RSSI</td><td>-48 dBm (Excellent)</td></tr>
                            <tr><td>Power Draw</td><td>7.4 W</td></tr>
                            <tr><td>mDNS Host</td><td>${device.cleanMdnsHost}</td></tr>
                        </table>
                        
                        <button onclick="document.getElementById('brightness').value=100;">100% Max Boost</button>
                        <button onclick="document.getElementById('brightness').value=20;">20% Cozy Dim</button>
                        <button onclick="alert('Timer set')">1h Sleep Timer</button>
                        <button onclick="location.reload()">Reboot ESP32</button>
                    </body>
                    </html>
                """.trimIndent()

                DeviceCategory.MUSIC, DeviceCategory.SPEAKER -> """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${device.name}</title></head>
                    <body>
                        <h2>${device.name} Audio DSP</h2>
                        <label><input type="checkbox" id="amp_power" checked data-label="Amplifier Power"> Amplifier Power</label>
                        <label><input type="checkbox" id="loudness_eq" checked data-label="3D Bass Boost"> 3D Bass Boost</label>
                        <label><input type="checkbox" id="mute_out" data-label="Mute Output"> Mute Output</label>
                        
                        <div>
                            <label for="master_vol">Master Volume</label>
                            <input type="range" id="master_vol" min="0" max="100" value="68" data-unit="%">
                        </div>
                        <div>
                            <label for="sub_bass">Subwoofer Bass</label>
                            <input type="range" id="sub_bass" min="-12" max="12" value="4" data-unit="dB">
                        </div>
                        <div>
                            <label for="treble_gain">Treble Clarity</label>
                            <input type="range" id="treble_gain" min="-12" max="12" value="2" data-unit="dB">
                        </div>
                        
                        <div>
                            <label for="audio_source">Input Source &amp; EQ</label>
                            <select id="audio_source">
                                <option value="i2s_dac" selected>Wi-Fi Lossless Stream (I2S)</option>
                                <option value="bt_a2dp">Bluetooth A2DP Sink</option>
                                <option value="spdif">Optical S/PDIF In</option>
                                <option value="aux">Studio Line-In</option>
                            </select>
                        </div>
                        
                        <table>
                            <tr><td>Sample Rate</td><td>96 kHz / 24-bit</td></tr>
                            <tr><td>DAC Temp</td><td>38.5 °C</td></tr>
                            <tr><td>Wi-Fi Signal</td><td>-44 dBm</td></tr>
                            <tr><td>mDNS Address</td><td>${device.cleanMdnsHost}</td></tr>
                        </table>
                        
                        <button>Play / Pause</button>
                        <button>Previous Track</button>
                        <button>Next Track</button>
                        <button>Reset Flat EQ</button>
                    </body>
                    </html>
                """.trimIndent()

                DeviceCategory.FAN_CLIMATE -> """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${device.name}</title></head>
                    <body>
                        <h2>${device.name} Climate Node</h2>
                        <label><input type="checkbox" id="hvac_power" checked data-label="Climate Power"> Climate Power</label>
                        <label><input type="checkbox" id="swing_osc" checked data-label="Auto Louver Swing"> Auto Louver Swing</label>
                        <label><input type="checkbox" id="eco_ion" data-label="Eco Purifier Mode"> Eco Purifier Mode</label>
                        
                        <div>
                            <label for="target_temp">Target Temperature</label>
                            <input type="range" id="target_temp" min="16" max="30" step="1" value="22" data-unit="°C">
                        </div>
                        <div>
                            <label for="fan_rpm">Fan Speed Level</label>
                            <input type="range" id="fan_rpm" min="0" max="100" step="5" value="60" data-unit="%">
                        </div>
                        
                        <div>
                            <label for="climate_mode">Operating Mode</label>
                            <select id="climate_mode">
                                <option value="cool" selected>Turbo Cool</option>
                                <option value="auto">Smart Auto</option>
                                <option value="dry">Dehumidify</option>
                                <option value="fan">Breeze Fan Only</option>
                            </select>
                        </div>
                        
                        <table>
                            <tr><td>Room Temperature</td><td>23.4 °C</td></tr>
                            <tr><td>Relative Humidity</td><td>48 %</td></tr>
                            <tr><td>Air Quality (AQI)</td><td>14 (Clean)</td></tr>
                            <tr><td>mDNS Host</td><td>${device.cleanMdnsHost}</td></tr>
                        </table>
                        
                        <button>Turbo Cool 15m</button>
                        <button>Silent Sleep Mode</button>
                        <button>Reset Filter Timer</button>
                    </body>
                    </html>
                """.trimIndent()

                else -> """
                    <!DOCTYPE html>
                    <html>
                    <head><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>${device.name}</title></head>
                    <body>
                        <h2>${device.name}</h2>
                        <label><input type="checkbox" id="relay_ch1" checked data-label="Relay Channel 1"> Relay Channel 1</label>
                        <label><input type="checkbox" id="relay_ch2" data-label="Relay Channel 2"> Relay Channel 2</label>
                        <label><input type="checkbox" id="status_led" checked data-label="Board Status LED"> Board Status LED</label>
                        
                        <div>
                            <label for="pwm_duty">PWM Output Duty</label>
                            <input type="range" id="pwm_duty" min="0" max="100" value="75" data-unit="%">
                        </div>
                        
                        <div>
                            <label for="node_profile">Automation Profile</label>
                            <select id="node_profile">
                                <option value="normal" selected>Standard Operation</option>
                                <option value="low_power">Deep Sleep Eco</option>
                                <option value="schedule">Timer Schedule</option>
                            </select>
                        </div>
                        
                        <table>
                            <tr><td>Free Heap</td><td>242 KB</td></tr>
                            <tr><td>Uptime</td><td>14d 06h 22m</td></tr>
                            <tr><td>Supply Voltage</td><td>5.02 V</td></tr>
                            <tr><td>mDNS Hostname</td><td>${device.cleanMdnsHost}</td></tr>
                        </table>
                        
                        <button>Pulse Relay 1s</button>
                        <button>Sync NTP Time</button>
                        <button>Reboot ESP32</button>
                    </body>
                    </html>
                """.trimIndent()
            }
        }
    }
}
