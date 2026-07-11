package com.android.purebilibili.feature.audio.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MusicPlayerContentStructureTest {

    @Test
    fun `compact player exposes liquid play lyrics segmented control`() {
        val source = loadSource()
        val compactBranch = source
            .substringAfter("MusicPlayerLayout.COMPACT_PAGER ->")
            .substringBefore("MusicPlayerLayout.EXPANDED_SPLIT ->")

        assertTrue(compactBranch.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(compactBranch.contains("listOf(\"播放\", \"歌词\")"))
        assertTrue(compactBranch.contains("indicatorPositionProvider"))
        assertTrue(compactBranch.contains("animateScrollToPage"))
        assertTrue(compactBranch.contains("navigationBarsPadding()"))
    }

    @Test
    fun `lyrics auto follow uses viewport offset and pauses for user drag`() {
        val source = loadSource()
        val lyricsPage = source.substringAfter("private fun LyricsPage(")

        assertTrue(lyricsPage.contains("collectIsDraggedAsState()"))
        assertTrue(lyricsPage.contains("resolveLyricFocusScrollOffsetPx("))
        assertTrue(lyricsPage.contains("delay(LYRIC_AUTO_FOLLOW_RESUME_DELAY_MS)"))
        assertTrue(!lyricsPage.contains("scrollToItem(currentIndex, -160)"))
        assertTrue(!lyricsPage.contains("animateScrollToItem(currentIndex, -160)"))
    }

    private fun loadSource(): String {
        val path = "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
