package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Person
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop as MiuixLayerBackdrop

internal data class CommentSortSegmentedControlSpec(
    val itemWidthDp: Int,
    val heightDp: Int,
    val indicatorHeightDp: Int
)

internal fun resolveCommentSortSegmentedControlSpec(itemCount: Int): CommentSortSegmentedControlSpec {
    return CommentSortSegmentedControlSpec(
        itemWidthDp = if (itemCount >= 4) 56 else 66,
        heightDp = AppChromeSizeTokens.InContentLiquidSegmentedControlHeightDp,
        indicatorHeightDp = AppChromeSizeTokens.InContentLiquidSegmentedIndicatorHeightDp,
    )
}

internal fun hasCommentSortIndicatorScaleClearance(
    containerHeightDp: Int,
    indicatorHeightDp: Int
): Boolean {
    // Same as bottom bar: drag scale may overflow; dock only needs room for the indicator band.
    return containerHeightDp > 0 && indicatorHeightDp > 0 &&
        containerHeightDp >= indicatorHeightDp
}

/**
 * 评论排序筛选栏
 * Header: "评论 (123)"
 * Controls: 底栏同源 Miuix 液态玻璃分段 [最热 | 最新 | ...]
 */
@Composable
fun CommentSortFilterBar(
    count: Int,
    sortMode: CommentSortMode,
    onSortModeChange: (CommentSortMode) -> Unit,
    upOnly: Boolean = false,
    onUpOnlyToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** Page content Miuix LayerBackdrop — same stack as home floating bottom bar. */
    miuixBackdrop: MiuixLayerBackdrop? = null,
) {
    val sortModes = remember { CommentSortMode.entries.toList() }
    val appearance = rememberVideoCommentAppearance()

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = "评论",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appearance.primaryTextColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            AppText(
                text = FormatUtils.formatStat(count.toLong()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = appearance.secondaryTextColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommentToggleButton(
                isChecked = upOnly,
                onToggle = onUpOnlyToggle,
                icon = CupertinoIcons.Filled.Person
            )

            CommentSegmentedControl(
                items = sortModes.map { it.label },
                selectedIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
                onScaleChange = { index ->
                    sortModes.getOrNull(index)?.let(onSortModeChange)
                },
                miuixBackdrop = miuixBackdrop
            )
        }
    }
}

/**
 * Bottom-bar Miuix liquid segmented control (no Kyant path).
 */
@Composable
fun CommentSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onScaleChange: (Int) -> Unit,
    miuixBackdrop: MiuixLayerBackdrop? = null
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val spec = remember(items.size) {
        resolveCommentSortSegmentedControlSpec(itemCount = items.size)
    }
    BottomBarLiquidSegmentedControl(
        items = items,
        selectedIndex = selectedIndex,
        onSelected = onScaleChange,
        itemWidth = spec.itemWidthDp.dp,
        height = spec.heightDp.dp,
        indicatorHeight = spec.indicatorHeightDp.dp,
        labelFontSize = 13.sp,
        // Prefer Miuix only — same stack as KernelSuAlignedBottomBar.
        miuixBackdrop = miuixBackdrop,
        forceLiquidChrome = homeSettings.androidNativeLiquidGlassEnabled,
        liquidGlassEffectsEnabled = miuixBackdrop != null,
    )
}

@Composable
fun CommentToggleButton(
    isChecked: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val appearance = rememberVideoCommentAppearance()
    val backgroundColor = if (isChecked) {
        appearance.toggleCheckedBackgroundColor
    } else {
        appearance.toggleUncheckedBackgroundColor
    }
    val contentColor = if (isChecked) {
        appearance.toggleCheckedContentColor
    } else {
        appearance.toggleUncheckedContentColor
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
