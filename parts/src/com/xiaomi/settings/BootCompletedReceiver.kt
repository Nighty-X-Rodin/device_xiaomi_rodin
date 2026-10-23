/*
 * SPDX-FileCopyrightText: 2018 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Display.HdrCapabilities
import com.xiaomi.settings.thermal.ThermalUtils

/** Restores XiaomiParts feature states at locked-boot. */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        if (DEBUG) Log.d(TAG, "LOCKED_BOOT_COMPLETED received")
        onLockedBootCompleted(context)
    }

    /**
     * Called early at locked-boot. Restores thermal override state and HDR capabilities.
     */
    private fun onLockedBootCompleted(context: Context) {
        // Restore thermal throttling state
        runCatching {
            ThermalUtils.getInstance(context.applicationContext).restoreStateAtBoot()
        }.onFailure { e -> Log.e(TAG, "Failed to restore thermal state", e) }

        // Force-enable all HDR types (Dolby Vision, HDR10, HLG, HDR10+)
        runCatching {
            context.getSystemService(DisplayManager::class.java)
                ?.overrideHdrTypes(
                    Display.DEFAULT_DISPLAY,
                    intArrayOf(
                        HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS,
                    ),
                )
        }.onFailure { e -> Log.e(TAG, "Failed to override HDR types", e) }
    }
}
