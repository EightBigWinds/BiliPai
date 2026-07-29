package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppChromeSizeTokenAdoptionTest {

    @Test
    fun `first batch capsule chrome surfaces read shared size tokens`() {
        val tokenizedSources = listOf(
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopTabStylePolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSortFilterBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LivePiliPlusVisualPolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveHomeCategoryIndicatorPolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/space/SpaceTabChromePolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListAppearancePolicy.kt"
        )

        tokenizedSources.forEach { path ->
            val source = loadSource(path)
            assertTrue(
                source.contains("resolveCompactCapsuleChromeSpec(") ||
                    source.contains("compactChromeSpec") ||
                    source.contains("CompactCapsuleChromeSpec") ||
                    source.contains("AppChromeSizeTokens"),
                "$path should use shared compact capsule chrome tokens"
            )
        }
    }

    @Test
    fun `liquid reuse segmented chrome uses bottom bar matched size tokens`() {
        val segmentedSources = listOf(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePiliPlusVisualPolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/live/LiveHomeCategoryIndicatorPolicy.kt",
            "app/src/main/java/com/android/purebilibili/feature/space/SpaceTabChromePolicy.kt"
        )

        segmentedSources.forEach { path ->
            val source = loadSource(path)
            assertTrue(
                source.contains("BottomBarMatchedSegmentedControlHeightDp") ||
                    source.contains("BottomBarMatchedSegmentedIndicatorHeightDp"),
                "$path should use bottom-bar matched size tokens"
            )
            assertFalse(
                source.contains("heightDp = compactChrome.primaryHeightDp"),
                "$path should not invent compact dock heights for liquid segmented controls"
            )
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
