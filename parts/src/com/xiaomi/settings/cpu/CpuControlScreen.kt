/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.cpu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xiaomi.settings.R
import com.xiaomi.settings.ui.Motion
import com.xiaomi.settings.ui.PartsInfoCard
import com.xiaomi.settings.ui.PartsPreferenceGroup
import com.xiaomi.settings.ui.PartsScaffold
import com.xiaomi.settings.ui.PartsSwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CpuControlScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { CpuControlManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var thermalThrottlingDisabled by remember { mutableStateOf(manager.isThermalThrottlingDisabled) }
    var applying by remember { mutableStateOf(false) }
    var showThrottleWarning by remember { mutableStateOf(false) }

    fun updateThrottling(disabled: Boolean) {
        if (applying) return
        scope.launch {
            applying = true
            withContext(Dispatchers.IO) {
                manager.setThermalThrottlingDisabled(disabled)
            }
            thermalThrottlingDisabled = manager.isThermalThrottlingDisabled
            applying = false
        }
    }

    PartsScaffold(
        title = stringResource(R.string.cpu_control_title),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "thermal-throttling") {
                PartsPreferenceGroup(title = stringResource(R.string.thermal_safety_label)) {
                    PartsSwitchPreference(
                        title = stringResource(R.string.thermal_disable_throttling_title),
                        summary = stringResource(R.string.thermal_disable_throttling_summary),
                        checked = thermalThrottlingDisabled,
                        icon = Icons.Filled.Warning,
                        enabled = !applying,
                        onCheckedChange = { disabled ->
                            if (disabled) {
                                showThrottleWarning = true
                            } else {
                                updateThrottling(false)
                            }
                        },
                    )
                }
                AnimatedVisibility(
                    visible = thermalThrottlingDisabled,
                    enter = expandVertically(animationSpec = Motion.defaultEffectsSpec()) + fadeIn(),
                    exit = shrinkVertically(animationSpec = Motion.defaultEffectsSpec()) + fadeOut(),
                ) {
                    PartsInfoCard(
                        title = stringResource(R.string.thermal_disable_warning_title).trimEnd('?'),
                        body = stringResource(R.string.thermal_disable_warning_body),
                        icon = Icons.Filled.Warning,
                        accent = true,
                    )
                }
            }
        }
    }

    if (showThrottleWarning) {
        AlertDialog(
            onDismissRequest = { showThrottleWarning = false },
            icon = {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = null)
            },
            title = {
                Text(
                    text = stringResource(R.string.thermal_disable_warning_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.thermal_disable_warning_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showThrottleWarning = false
                        updateThrottling(true)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.thermal_disable_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showThrottleWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
