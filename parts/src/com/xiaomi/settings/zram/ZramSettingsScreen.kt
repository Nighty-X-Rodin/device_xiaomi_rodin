/*
 * SPDX-FileCopyrightText: 2026 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.zram

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaomi.settings.R
import com.xiaomi.settings.ui.IconTile
import com.xiaomi.settings.ui.PageGutter
import com.xiaomi.settings.ui.PressableCard
import com.xiaomi.settings.ui.SectionHeader
import com.xiaomi.settings.utils.PartsToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZramSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var zramInfo by remember { mutableStateOf(ZramUtils.readZramInfo()) }
    var selectedPercent by remember { mutableIntStateOf(zramInfo.percent) }
    var sliderValue by remember { mutableFloatStateOf(zramInfo.percent.toFloat()) }
    var isApplying by remember { mutableStateOf(false) }

    fun refresh() {
        zramInfo = ZramUtils.readZramInfo()
    }

    LaunchedEffect(Unit) {
        refresh()
        selectedPercent = zramInfo.percent
        sliderValue = zramInfo.percent.toFloat()
    }

    val targetMb = ZramUtils.calculateMbForPercent(zramInfo.totalRamKb, selectedPercent)
    val hasPendingChanges = selectedPercent != zramInfo.percent

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.zram_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_up))
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Live Hero Gauge ──────────────────────────────────────────
            HeroZramCard(
                zramInfo = zramInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter),
            )

            // ── Presets Grid ──────────────────────────────────────────────
            SectionHeader(
                title = stringResource(R.string.zram_percentage),
                modifier = Modifier.padding(horizontal = PageGutter),
            )

            val presets = listOf(
                25 to stringResource(R.string.zram_preset_basic),
                50 to stringResource(R.string.zram_preset_balanced),
                65 to stringResource(R.string.zram_preset_performance),
                80 to stringResource(R.string.zram_preset_heavy),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { (pct, label) ->
                    val isSelected = selectedPercent == pct
                    val presetMb = ZramUtils.calculateMbForPercent(zramInfo.totalRamKb, pct)
                    val presetSizeStr = ZramUtils.formatBytes(presetMb * 1024 * 1024)

                    PresetOptionCard(
                        percent = pct,
                        label = label,
                        sizeStr = presetSizeStr,
                        isSelected = isSelected,
                        onClick = {
                            selectedPercent = pct
                            sliderValue = pct.toFloat()
                        },
                    )
                }
            }

            // ── Custom Slider ─────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Ajuste preciso",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "$selectedPercent%  (${ZramUtils.formatBytes(targetMb * 1024 * 1024)})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            selectedPercent = it.roundToInt()
                        },
                        valueRange = 10f..80f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "10% (Mín)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "50% (Recomendado)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "80% (Máx)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Apply Button ──────────────────────────────────────────────
            Button(
                onClick = {
                    if (isApplying) return@Button
                    scope.launch {
                        isApplying = true
                        val success = ZramUtils.setZramPercent(selectedPercent)
                        if (success) {
                            refresh()
                            val updatedInfo = ZramUtils.readZramInfo()
                            val currentMb = updatedInfo.zramDiskSizeBytes / (1024 * 1024)
                            val appliedNow = kotlin.math.abs(currentMb - targetMb) <= 64
                            if (appliedNow) {
                                PartsToast.show(
                                    context,
                                    context.getString(
                                        R.string.zram_applied,
                                        ZramUtils.formatBytes(targetMb * 1024 * 1024),
                                    ),
                                )
                            } else {
                                PartsToast.show(
                                    context,
                                    context.getString(R.string.zram_applied_reboot),
                                )
                            }
                        } else {
                            PartsToast.show(context, R.string.zram_failed)
                        }
                        isApplying = false
                    }
                },
                enabled = hasPendingChanges && !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.zram_applying))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasPendingChanges) {
                            stringResource(R.string.zram_apply) + " ($selectedPercent%)"
                        } else {
                            "Aplicado"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // ── Information Card ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter)
                    .padding(bottom = 32.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.zram_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroZramCard(
    zramInfo: ZramUtils.ZramInfo,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(
                        icon = Icons.Default.SwapVert,
                        container = MaterialTheme.colorScheme.primary,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.zram_disksize),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = ZramUtils.formatBytes(zramInfo.zramDiskSizeBytes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ) {
                    Text(
                        text = zramInfo.algorithm.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.zram_usage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "${ZramUtils.formatKb(zramInfo.swapUsedKb)} / ${ZramUtils.formatKb(zramInfo.swapTotalKb)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                val animatedFraction by animateFloatAsState(
                    targetValue = zramInfo.swapUsedFraction,
                    label = "swapUsedFraction",
                )

                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun PresetOptionCard(
    percent: Int,
    label: String,
    sizeStr: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "presetBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "presetContainer",
    )

    PressableCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Text(
                    text = sizeStr,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
