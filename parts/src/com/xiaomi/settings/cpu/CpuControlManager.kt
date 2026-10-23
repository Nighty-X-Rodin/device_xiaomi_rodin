/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.cpu

import android.content.Context
import com.xiaomi.settings.thermal.ThermalUtils

/**
 * Manages CPU thermal throttling safety override.
 */
class CpuControlManager(context: Context) {

    private val thermalUtils = ThermalUtils.getInstance(context.applicationContext)

    val isThermalThrottlingDisabled: Boolean
        get() = thermalUtils.throttlingDisabled

    fun setThermalThrottlingDisabled(disabled: Boolean): Boolean {
        thermalUtils.throttlingDisabled = disabled
        return true
    }
}
