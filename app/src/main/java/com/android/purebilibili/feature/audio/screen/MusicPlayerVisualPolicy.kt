package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.LyricDocument
import kotlin.math.abs

internal data class MusicLyricFocusStyle(
    val blurRadiusDp: Int,
    val alphaPercent: Int
)

internal data class MusicGlassFallbackStyle(
    val backgroundAlphaPercent: Int,
    val borderAlphaPercent: Int
)

internal fun resolveMusicPagerIndicatorPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float
): Float = (currentPage + currentPageOffsetFraction).coerceIn(0f, 1f)

internal fun resolveMusicGlassFallbackStyle(): MusicGlassFallbackStyle {
    return MusicGlassFallbackStyle(
        backgroundAlphaPercent = 48,
        borderAlphaPercent = 24
    )
}

internal fun resolveLyricFocusScrollOffsetPx(viewportHeightPx: Int): Int {
    if (viewportHeightPx <= 0) return 0
    return -(viewportHeightPx * 0.30f).toInt()
}

internal fun resolveMusicLyricsBlurEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    reduceMotion: Boolean
): Boolean = sdkInt >= 31 && effectsEnabled && !reduceMotion

internal fun resolveMusicLyricFocusStyle(
    lineIndex: Int,
    currentIndex: Int,
    blurEnabled: Boolean
): MusicLyricFocusStyle {
    val distance = abs(lineIndex - currentIndex)
    val alphaPercent = when (distance) {
        0 -> 100
        1 -> 46
        2 -> 30
        else -> 20
    }
    val blurRadiusDp = if (!blurEnabled) {
        0
    } else {
        when (distance) {
            0 -> 0
            1 -> 2
            2 -> 4
            else -> 7
        }
    }
    return MusicLyricFocusStyle(blurRadiusDp, alphaPercent)
}

internal fun resolveMusicLiquidGlassEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    isAppInBackground: Boolean,
    reduceMotion: Boolean
): Boolean {
    return effectsEnabled &&
        sdkInt in 33..35 &&
        !isAppInBackground &&
        !reduceMotion
}

internal fun resolveCurrentLyricIndex(
    document: LyricDocument,
    positionMs: Long
): Int {
    if (document.lines.isEmpty()) return -1
    val adjustedPosition = positionMs - document.offsetMs
    var low = 0
    var high = document.lines.lastIndex
    var result = -1
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (document.lines[middle].startTimeMs <= adjustedPosition) {
            result = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    for (index in result downTo 0) {
        val line = document.lines[index]
        if (adjustedPosition >= line.startTimeMs && adjustedPosition < line.endTimeMs) {
            return index
        }
    }
    return -1
}
