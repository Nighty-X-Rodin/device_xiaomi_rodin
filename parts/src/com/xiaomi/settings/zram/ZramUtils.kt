/*
 * SPDX-FileCopyrightText: 2026 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.zram

import android.os.SystemProperties
import com.xiaomi.settings.utils.readOneLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object ZramUtils {
    const val PROP_ZRAM_PCT = "persist.sys.zram_size_percent"
    private const val SYSFS_ZRAM_DISKSIZE = "/sys/block/zram0/disksize"
    private const val SYSFS_ZRAM_COMP_ALGO = "/sys/block/zram0/comp_algorithm"

    data class ZramInfo(
        val totalRamKb: Long,
        val zramDiskSizeBytes: Long,
        val swapTotalKb: Long,
        val swapFreeKb: Long,
        val percent: Int,
        val algorithm: String,
    ) {
        val swapUsedKb: Long get() = (swapTotalKb - swapFreeKb).coerceAtLeast(0L)
        val swapUsedFraction: Float
            get() = if (swapTotalKb > 0) swapUsedKb.toFloat() / swapTotalKb.toFloat() else 0f
    }

    fun readZramInfo(): ZramInfo {
        var totalRamKb = 0L
        var swapTotalKb = 0L
        var swapFreeKb = 0L

        runCatching {
            File("/proc/meminfo").forEachLine { line ->
                val parts = line.split(Regex(":\\s+"))
                if (parts.size >= 2) {
                    val key = parts[0].trim()
                    val value = parts[1].substringBefore(' ').trim().toLongOrNull() ?: 0L
                    when (key) {
                        "MemTotal" -> totalRamKb = value
                        "SwapTotal" -> swapTotalKb = value
                        "SwapFree" -> swapFreeKb = value
                    }
                }
            }
        }

        val diskSizeBytes = readOneLine(SYSFS_ZRAM_DISKSIZE)?.trim()?.toLongOrNull() ?: 0L
        val rawAlgo = readOneLine(SYSFS_ZRAM_COMP_ALGO)?.trim() ?: "lz4"
        val algo = if (rawAlgo.contains("[")) {
            rawAlgo.substringAfter("[").substringBefore("]")
        } else {
            rawAlgo
        }

        val currentPct = SystemProperties.getInt(PROP_ZRAM_PCT, 50).coerceIn(10, 80)

        return ZramInfo(
            totalRamKb = totalRamKb,
            zramDiskSizeBytes = diskSizeBytes,
            swapTotalKb = swapTotalKb,
            swapFreeKb = swapFreeKb,
            percent = currentPct,
            algorithm = algo,
        )
    }

    suspend fun setZramPercent(targetPct: Int): Boolean = withContext(Dispatchers.IO) {
        val pct = targetPct.coerceIn(10, 80)
        runCatching {
            SystemProperties.set(PROP_ZRAM_PCT, pct.toString())
            delay(1200)
            true
        }.getOrDefault(false)
    }

    fun calculateMbForPercent(totalRamKb: Long, percent: Int): Long {
        if (totalRamKb <= 0) return 0L
        val mb = (totalRamKb * percent.coerceIn(10, 80)) / 100 / 1024
        return mb.coerceAtLeast(16L)
    }

    fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) "%.1f GB".format(gb) else "${bytes / (1024 * 1024)} MB"
    }

    fun formatKb(kb: Long): String {
        return formatBytes(kb * 1024)
    }
}
