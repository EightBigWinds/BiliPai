package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppNativeSegmentedControl
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppSegmentedChrome
import com.android.purebilibili.core.ui.components.resolveAppLiquidSegmentedControlSpec
import com.android.purebilibili.core.ui.components.resolveAppSegmentedChrome
import com.android.purebilibili.core.ui.components.resolveAppSegmentedSelectionIndex
import com.android.purebilibili.core.ui.rememberAppSegmentedControlPolicy
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop

@Composable
internal fun <T> AppSegmentedPreference(
    title: String,
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit,
) {
    val policy = rememberAppSegmentedControlPolicy()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = title,
            style = if (policy.usesEmphasizedTitle) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = AppSurfaceTokens.onSurface(),
        )
        if (!subtitle.isNullOrBlank()) {
            AppText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppSurfaceTokens.onSurfaceVariantSummary(),
            )
        }
        AppSegmentedControl(
            options = options,
            selectedValue = selectedValue,
            enabled = enabled,
            onSelectionChange = onSelectionChange,
        )
    }
}

@Composable
internal fun <T> AppSegmentedControl(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    forceLiquidIndicator: Boolean = false,
    // In-content flexible defaults; rendering still follows bottom-bar indicator path.
    height: Dp = AppChromeSizeTokens.InContentLiquidSegmentedControlHeightDp.dp,
    indicatorHeight: Dp = AppChromeSizeTokens.InContentLiquidSegmentedIndicatorHeightDp.dp,
    labelFontSize: TextUnit = 14.sp,
    backdrop: Backdrop? = null,
    /** Preferred page capture — same Miuix stack as KernelSuAlignedBottomBar. */
    miuixBackdrop: MiuixBackdrop? = null,
    tapPressRefractionEnabled: Boolean = true,
    containerColorOverride: Color? = null,
    indicatorIdleSurfaceColorOverride: Color? = null,
    onSelectionChange: (T) -> Unit,
) {
    if (options.isEmpty()) return
    val policy = rememberAppSegmentedControlPolicy()
    val context = androidx.compose.ui.platform.LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val nativeLiquidGlassEnabled =
        forceLiquidIndicator || homeSettings.androidNativeLiquidGlassEnabled

    when (
        resolveAppSegmentedChrome(
            usesMaterialFallback = policy.usesMaterialFallback,
            nativeLiquidGlassEnabled = nativeLiquidGlassEnabled,
        )
    ) {
        AppSegmentedChrome.NATIVE -> AppNativeSegmentedControl(
            options = options,
            selectedValue = selectedValue,
            modifier = modifier,
            enabled = enabled,
            onSelectionChange = onSelectionChange,
        )
        AppSegmentedChrome.LIQUID -> AppLiquidSegmentedControlHost(
            options = options,
            selectedValue = selectedValue,
            modifier = modifier,
            enabled = enabled,
            forceLiquidIndicator = forceLiquidIndicator,
            height = height,
            indicatorHeight = indicatorHeight,
            labelFontSize = labelFontSize,
            backdrop = backdrop,
            miuixBackdrop = miuixBackdrop,
            tapPressRefractionEnabled = tapPressRefractionEnabled,
            containerColorOverride = containerColorOverride,
            indicatorIdleSurfaceColorOverride = indicatorIdleSurfaceColorOverride,
            onSelectionChange = onSelectionChange,
        )
    }
}

@Composable
private fun <T> AppLiquidSegmentedControlHost(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier,
    enabled: Boolean,
    forceLiquidIndicator: Boolean,
    height: Dp,
    indicatorHeight: Dp,
    labelFontSize: TextUnit,
    backdrop: Backdrop?,
    miuixBackdrop: MiuixBackdrop?,
    tapPressRefractionEnabled: Boolean,
    containerColorOverride: Color?,
    indicatorIdleSurfaceColorOverride: Color?,
    onSelectionChange: (T) -> Unit,
) {
    val selectedIndex = resolveAppSegmentedSelectionIndex(options, selectedValue)
    val longestLabelLength = options.maxOfOrNull { it.label.length } ?: 0
    val spec = resolveAppLiquidSegmentedControlSpec(
        itemCount = options.size,
        hasExternalBackdrop = miuixBackdrop != null || backdrop != null,
        longestLabelLength = longestLabelLength,
    )
    // Defaults use in-content sizes; when caller still passes them (or pure defaults),
    // apply policy-resolved geometry + keep bottom-bar press refraction.
    val usesDefaultInContentSizing =
        height == AppChromeSizeTokens.InContentLiquidSegmentedControlHeightDp.dp &&
            indicatorHeight == AppChromeSizeTokens.InContentLiquidSegmentedIndicatorHeightDp.dp &&
            labelFontSize == 14.sp
    val resolvedHeight = if (usesDefaultInContentSizing) spec.heightDp.dp else height
    val resolvedIndicatorHeight =
        if (usesDefaultInContentSizing) spec.indicatorHeightDp.dp else indicatorHeight
    val resolvedLabelFontSize =
        if (usesDefaultInContentSizing) spec.labelFontSizeSp.sp else labelFontSize
    val resolvedTapPressRefractionEnabled =
        if (usesDefaultInContentSizing) spec.tapPressRefractionEnabled else tapPressRefractionEnabled

    BottomBarLiquidSegmentedControl(
        items = options.map { it.label },
        selectedIndex = selectedIndex,
        onSelected = { index ->
            options.getOrNull(index)?.let { onSelectionChange(it.value) }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        itemWidth = null,
        height = resolvedHeight,
        indicatorHeight = resolvedIndicatorHeight,
        labelFontSize = resolvedLabelFontSize,
        backdrop = backdrop,
        miuixBackdrop = miuixBackdrop,
        forceLiquidChrome = forceLiquidIndicator,
        liquidGlassEffectsEnabled = spec.liquidGlassEffectsEnabled,
        tapPressRefractionEnabled = resolvedTapPressRefractionEnabled,
        containerColorOverride = containerColorOverride,
        indicatorIdleSurfaceColorOverride = indicatorIdleSurfaceColorOverride,
    )
}
