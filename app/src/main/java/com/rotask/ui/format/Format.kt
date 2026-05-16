package com.rotask.ui.format

import kotlin.math.abs

fun formatClock(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatWeight(weight: Double): String {
    val rounded = (weight * 10).toLong() / 10.0
    val isWhole = abs(rounded - rounded.toLong()) < 0.001
    return if (isWhole) "x${rounded.toLong()}" else "x%.1f".format(rounded)
}
