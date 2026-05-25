package com.android.purebilibili.navigation3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val NAV3_FALLBACK_FADE_MILLIS = 180
private const val NAV3_DISABLED_VIDEO_DIRECTION_MILLIS = 220
internal fun resolveBiliPaiNavContentTransform(
    routeTransition: BiliPaiNavRouteTransition
): ContentTransform {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT ->
            EnterTransition.None togetherWith ExitTransition.None
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT ->
            disabledVideoDirectionForwardTransform(directionSign = -1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT ->
            disabledVideoDirectionForwardTransform(directionSign = 1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT ->
            disabledVideoDirectionReturnTransform(directionSign = -1)
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ->
            disabledVideoDirectionReturnTransform(directionSign = 1)
        BiliPaiNavRouteTransition.CLASSIC_CARD,
        BiliPaiNavRouteTransition.FALLBACK ->
            fadeIn(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS)) togetherWith
                fadeOut(animationSpec = tween(NAV3_FALLBACK_FADE_MILLIS))
    }
}

internal fun resolveBiliPaiNavPopContentTransform(
    routeTransition: BiliPaiNavRouteTransition
): ContentTransform? {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ->
            resolveBiliPaiNavContentTransform(routeTransition)
        else -> null
    }
}

private fun disabledVideoDirectionForwardTransform(directionSign: Int): ContentTransform {
    return (
        slideInHorizontally(
            animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS),
            initialOffsetX = { width -> directionSign * width / 4 }
        ) + fadeIn(animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS))
    ) togetherWith fadeOut(animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS))
}

private fun disabledVideoDirectionReturnTransform(directionSign: Int): ContentTransform {
    return EnterTransition.None togetherWith
        (
            slideOutHorizontally(
                animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS),
                targetOffsetX = { width -> directionSign * width / 4 }
            ) + fadeOut(animationSpec = tween(NAV3_DISABLED_VIDEO_DIRECTION_MILLIS))
        )
}
