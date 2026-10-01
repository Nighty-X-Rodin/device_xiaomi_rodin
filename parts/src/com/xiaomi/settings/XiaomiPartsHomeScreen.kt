/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-FileCopyrightText: 2026 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaomi.settings.monitor.MemInfo
import com.xiaomi.settings.monitor.findZone
import com.xiaomi.settings.monitor.readMemInfo
import com.xiaomi.settings.monitor.readThermalZones
import com.xiaomi.settings.thermal.ThermalUtils
import com.xiaomi.settings.touchsampling.TouchSamplingService
import com.xiaomi.settings.ui.IconTile
import com.xiaomi.settings.ui.PageGutter
import com.xiaomi.settings.ui.PressableCard
import com.xiaomi.settings.utils.CitLauncher
import com.xiaomi.settings.utils.PartsToast
import com.xiaomi.settings.zram.ZramUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class HomeBatterySnap(
    val level: Int,
    val plugged: Int,
    val tempTenths: Int,
)

private fun readHomeBattery(context: Context): HomeBatterySnap {
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return HomeBatterySnap(
        level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
        plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
        tempTenths = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0,
    )
}

private data class DisplaySummary(
    val resolutionLabel: String,
    val refreshRateHz: Int,
)

private fun getDisplaySummary(context: Context): DisplaySummary {
    return runCatching {
        val wm = context.getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        val display = context.display ?: wm?.defaultDisplay
        val mode = display?.mode
        val hz = mode?.refreshRate?.roundToInt() ?: 120
        val width = mode?.physicalWidth ?: 1220
        val height = mode?.physicalHeight ?: 2712
        val label = when {
            width >= 1440 || height >= 3100 -> "1440p QHD+"
            width >= 1220 || height >= 2700 -> "1220p Native"
            width >= 1080 || height >= 2300 -> "1080p FHD+"
            else -> "${minOf(width, height)}p"
        }
        DisplaySummary(label, hz)
    }.getOrDefault(DisplaySummary("1220p Native", 120))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaomiPartsHomeScreen(
    onNavigateToDisplay: () -> Unit,
    onNavigateToResolution: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToTouch: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToZram: () -> Unit,
) {
    val context = LocalContext.current
    val horizontalGutter = PageGutter
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var batterySnap by remember { mutableStateOf(readHomeBattery(context)) }
    var thermalZones by remember { mutableStateOf(readThermalZones()) }
    var memInfo by remember { mutableStateOf(readMemInfo()) }
    var zramInfo by remember { mutableStateOf(ZramUtils.readZramInfo()) }
    var touchBoostEnabled by remember { mutableStateOf(TouchSamplingService.isEnabled(context)) }
    val displaySummary by remember { mutableStateOf(getDisplaySummary(context)) }
    val thermalUtils = remember { ThermalUtils.getInstance(context) }

    val socZone = remember(thermalZones) {
        findZone(thermalZones, listOf("soc", "cpu", "tslocal", "mtkts")) ?: thermalZones.firstOrNull()
    }
    val socTempC = socZone?.tempC

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000)
            batterySnap = readHomeBattery(context)
            thermalZones = readThermalZones()
            memInfo = readMemInfo()
            zramInfo = ZramUtils.readZramInfo()
            touchBoostEnabled = TouchSamplingService.isEnabled(context)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                batterySnap = readHomeBattery(ctx)
            }
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.xiaomi_parts_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 1. Hero Dashboard Card (Live Stats) ───────────────────
            item(key = "hero-dashboard") {
                HeroDashboardCard(
                    batterySnap = batterySnap,
                    socTempC = socTempC,
                    displaySummary = displaySummary,
                    zramInfo = zramInfo,
                    horizontalGutter = horizontalGutter,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToResolution = onNavigateToResolution,
                    onNavigateToZram = onNavigateToZram,
                )
            }

            // ── 2. Quick Actions ─────────────────────────────────────
            item(key = "quick-actions-label") {
                PartsCategory(stringResource(R.string.quick_actions_title))
            }
            item(key = "quick-action-touch") {
                TouchBoostQuickCard(
                    enabled = touchBoostEnabled,
                    onCheckedChange = { newVal ->
                        runCatching {
                            TouchSamplingService.setEnabled(context, newVal)
                            touchBoostEnabled = newVal
                        }.onFailure {
                            PartsToast.show(context, R.string.touch_boost_failed)
                        }
                    },
                    onClick = onNavigateToTouch,
                    horizontalGutter = horizontalGutter,
                )
            }

            // ── 3. Performance & Memory Bento Grid ───────────────────
            item(key = "perf-memory-label") {
                PartsCategory(stringResource(R.string.performance_category))
            }
            item(key = "perf-bento-row-1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ZramBentoCard(
                        zramInfo = zramInfo,
                        onClick = onNavigateToZram,
                        modifier = Modifier.weight(1.15f),
                    )
                    CompactBentoTile(
                        icon = ImageVector.vectorResource(R.drawable.ic_thermal_settings),
                        title = stringResource(R.string.bento_thermal_title),
                        subtitle = stringResource(R.string.thermal_summary),
                        chipText = if (thermalUtils.enabled) stringResource(R.string.status_active) else stringResource(R.string.status_disabled),
                        onClick = onNavigateToThermal,
                        modifier = Modifier.weight(0.85f),
                    )
                }
            }
            item(key = "perf-bento-row-2") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CompactBentoTile(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.bento_performance_title),
                        subtitle = stringResource(R.string.performance_summary),
                        chipText = "CPU/GPU",
                        onClick = onNavigateToPerformance,
                        modifier = Modifier.weight(1f),
                    )
                    val freeRamText = if (memInfo != null) {
                        "${"%.1f".format(memInfo!!.availableKb / (1024.0 * 1024.0))} GB free"
                    } else {
                        stringResource(R.string.memory_summary)
                    }
                    CompactBentoTile(
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.bento_memory_title),
                        subtitle = freeRamText,
                        chipText = "RAM",
                        onClick = onNavigateToMemory,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── 4. Display Bento Block ───────────────────────────────
            item(key = "display-label") {
                PartsCategory(stringResource(R.string.display_category))
            }
            item(key = "display-bento-row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CompactBentoTile(
                        icon = ImageVector.vectorResource(R.drawable.ic_display_colours),
                        title = stringResource(R.string.bento_colours_title),
                        subtitle = stringResource(R.string.display_colours_summary),
                        onClick = onNavigateToDisplay,
                        modifier = Modifier.weight(1f),
                    )
                    CompactBentoTile(
                        icon = ImageVector.vectorResource(R.drawable.ic_screen_resolution),
                        title = stringResource(R.string.bento_resolution_title),
                        subtitle = displaySummary.resolutionLabel,
                        chipText = "${displaySummary.refreshRateHz}Hz",
                        onClick = onNavigateToResolution,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── 5. System & Battery ──────────────────────────────────
            item(key = "system-label") {
                PartsCategory(stringResource(R.string.system_category))
            }
            item(key = "system-battery-card") {
                val batSummary = if (batterySnap.level >= 0) {
                    "${batterySnap.level}% • ${if (batterySnap.plugged != 0) "Charging" else "On battery"}"
                } else {
                    stringResource(R.string.battery_status_summary)
                }
                WideBentoCard(
                    icon = Icons.Filled.BatteryChargingFull,
                    title = stringResource(R.string.battery_status_title),
                    summary = batSummary,
                    badgeText = "${batterySnap.tempTenths / 10.0}°C",
                    horizontalGutter = horizontalGutter,
                    onClick = onNavigateToBattery,
                )
            }

            // ── 6. Hardware Diagnostics ──────────────────────────────
            item(key = "diag-label") {
                PartsCategory(stringResource(R.string.xiaomi_parts_category_diagnostics))
            }
            item(key = "diag-bento-row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CompactBentoTile(
                        icon = Icons.Filled.Fingerprint,
                        title = stringResource(R.string.bento_fingerprint_title),
                        subtitle = stringResource(R.string.optical_sensor_chip),
                        onClick = {
                            if (!CitLauncher.launchFingerprintCalibration(context)) {
                                PartsToast.show(context, R.string.fingerprint_calibration_not_found)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    CompactBentoTile(
                        icon = Icons.Filled.Speaker,
                        title = stringResource(R.string.bento_speaker_title),
                        subtitle = stringResource(R.string.audio_cit_chip),
                        onClick = {
                            if (!CitLauncher.launchSpeakerCalibration(context)) {
                                PartsToast.show(context, R.string.speaker_calibration_not_found)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun PartsCategory(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun HeroDashboardCard(
    batterySnap: HomeBatterySnap,
    socTempC: Float?,
    displaySummary: DisplaySummary,
    zramInfo: ZramUtils.ZramInfo,
    horizontalGutter: Dp,
    onNavigateToBattery: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToResolution: () -> Unit,
    onNavigateToZram: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalGutter),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    val deviceModel = remember {
                        runCatching {
                            val marketName = android.os.SystemProperties.get("ro.product.marketname", "")
                            if (marketName.isNotBlank()) marketName else android.os.Build.MODEL
                        }.getOrDefault("POCO X7 Pro")
                    }
                    Text(
                        text = if (deviceModel.isNotBlank()) deviceModel else stringResource(R.string.device_overview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.device_overview_chip),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = if (batterySnap.plugged != 0) Icons.Filled.Bolt else Icons.Filled.BatteryFull,
                    iconTint = if (batterySnap.plugged != 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.bento_battery_title),
                    value = if (batterySnap.level >= 0) "${batterySnap.level}%" else "—",
                    sub = if (batterySnap.plugged != 0) "Charging" else "${batterySnap.tempTenths / 10.0}°C",
                    onClick = onNavigateToBattery,
                )
                val tempColor = when {
                    socTempC == null -> MaterialTheme.colorScheme.primary
                    socTempC < 42f -> MaterialTheme.colorScheme.primary
                    socTempC < 55f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                HeroMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.DeviceThermostat,
                    iconTint = tempColor,
                    label = stringResource(R.string.bento_soc_thermal_title),
                    value = if (socTempC != null) "${"%.1f".format(socTempC)} °C" else "Optimal",
                    sub = when {
                        socTempC == null -> "Dimensity"
                        socTempC < 42f -> "Cool"
                        socTempC < 55f -> "Warm"
                        else -> "High load"
                    },
                    onClick = onNavigateToThermal,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = ImageVector.vectorResource(R.drawable.ic_screen_resolution),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.bento_display_title),
                    value = displaySummary.resolutionLabel.substringBefore(" "),
                    sub = "${displaySummary.refreshRateHz} Hz AMOLED",
                    onClick = onNavigateToResolution,
                )
                val zramGb = "%.1f".format(zramInfo.swapTotalKb / (1024.0 * 1024.0))
                HeroMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Memory,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.bento_zram_title),
                    value = "$zramGb GB",
                    sub = "${zramInfo.percent}% RAM Swap",
                    onClick = onNavigateToZram,
                )
            }
        }
    }
}

@Composable
private fun HeroMetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    sub: String,
    onClick: () -> Unit,
) {
    PressableCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TouchBoostQuickCard(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    horizontalGutter: Dp,
) {
    PressableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalGutter),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconTile(
                icon = ImageVector.vectorResource(R.drawable.ic_touch_boost),
                container = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.touch_boost_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (enabled) stringResource(R.string.status_active)
                    else stringResource(R.string.touch_boost_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun ZramBentoCard(
    zramInfo: ZramUtils.ZramInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressableCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = Icons.Filled.SwapVert,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${zramInfo.percent}% RAM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.zram_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                val zramGb = "%.1f".format(zramInfo.swapTotalKb / (1024.0 * 1024.0))
                Text(
                    text = "$zramGb GB compressed swap",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { (zramInfo.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CompactBentoTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    chipText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressableCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(
                    icon = icon,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (chipText != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WideBentoCard(
    icon: ImageVector,
    title: String,
    summary: String,
    badgeText: String? = null,
    horizontalGutter: Dp = PageGutter,
    onClick: () -> Unit,
) {
    PressableCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalGutter),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconTile(
                icon = icon,
                container = MaterialTheme.colorScheme.primaryContainer,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
