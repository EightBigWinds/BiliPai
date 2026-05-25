package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.AppSystemBackAction

internal enum class BiliPaiNavMotionMode {
    CARD_DISABLED,
    CLASSIC_CARD
}

internal enum class BiliPaiNavRouteTransition {
    NO_OP_SHARED_ELEMENT,
    CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT,
    CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT,
    CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
    CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
    CLASSIC_CARD,
    FALLBACK
}

internal data class BiliPaiNavMotionDecision(
    val mode: BiliPaiNavMotionMode,
    val routeTransition: BiliPaiNavRouteTransition,
    val interceptSystemBack: Boolean
)

internal data class BiliPaiBackGestureDecision(
    val routeTransition: BiliPaiNavRouteTransition,
    val interceptSystemBack: Boolean
)

internal fun resolveBiliPaiNavMotionMode(
    cardTransitionEnabled: Boolean
): BiliPaiNavMotionMode {
    return if (cardTransitionEnabled) {
        BiliPaiNavMotionMode.CLASSIC_CARD
    } else {
        BiliPaiNavMotionMode.CARD_DISABLED
    }
}

internal fun resolveBiliPaiNavMotionDecision(
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    cardTransitionEnabled: Boolean,
    sharedTransitionReady: Boolean,
    appBackActionRequiresInterception: Boolean = false
): BiliPaiNavMotionDecision {
    val mode = resolveBiliPaiNavMotionMode(cardTransitionEnabled = cardTransitionEnabled)
    val isVideoToCardReturn = fromKey is BiliPaiNavKey.VideoDetail &&
        toKey != null &&
        isCardReturnTargetNavKey(toKey)
    val isCardToVideoForward = fromKey != null &&
        isCardReturnTargetNavKey(fromKey) &&
        toKey is BiliPaiNavKey.VideoDetail
    val routeTransition = when {
        cardTransitionEnabled &&
            sharedTransitionReady &&
            (isVideoToCardReturn || isCardToVideoForward) ->
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        mode == BiliPaiNavMotionMode.CLASSIC_CARD ->
            BiliPaiNavRouteTransition.CLASSIC_CARD
        else -> BiliPaiNavRouteTransition.FALLBACK
    }

    return BiliPaiNavMotionDecision(
        mode = mode,
        routeTransition = routeTransition,
        interceptSystemBack = shouldInterceptSystemBackForNavigation3(
            mode = mode,
            appBackActionRequiresInterception = appBackActionRequiresInterception
        )
    )
}

internal fun resolveBiliPaiBackGestureDecision(
    cardTransitionEnabled: Boolean,
    systemBackAction: AppSystemBackAction,
    currentKey: BiliPaiNavKey?,
    previousKey: BiliPaiNavKey?,
    sourceMetadata: BiliPaiNavSourceMetadata
): BiliPaiBackGestureDecision {
    val motionMode = resolveBiliPaiNavMotionMode(cardTransitionEnabled = cardTransitionEnabled)
    val routeTransition = resolveBiliPaiNavDisplayPopRouteTransition(
        cardTransitionEnabled = cardTransitionEnabled,
        sourceMetadata = sourceMetadata,
        fromKey = currentKey,
        toKey = previousKey
    )
    val isAppAction = systemBackAction == AppSystemBackAction.RETURN_TO_HOME_TAB
    return BiliPaiBackGestureDecision(
        routeTransition = if (isAppAction) {
            BiliPaiNavRouteTransition.FALLBACK
        } else {
            routeTransition
        },
        interceptSystemBack = isAppAction || motionMode == BiliPaiNavMotionMode.CLASSIC_CARD
    )
}

internal fun resolveBiliPaiNavDisplayPopRouteTransition(
    cardTransitionEnabled: Boolean = true,
    sourceMetadata: BiliPaiNavSourceMetadata,
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?
): BiliPaiNavRouteTransition {
    val fromVideoKey = fromKey as? BiliPaiNavKey.VideoDetail
    val normalizedSourceRoute = sourceMetadata.sourceRoute?.substringBefore("?")
    val normalizedVideoRoute = fromVideoKey?.sourceRoute?.substringBefore("?")
    val sourceMatchesCurrentVideo = fromVideoKey != null &&
        normalizedSourceRoute != null &&
        normalizedVideoRoute == normalizedSourceRoute &&
        sourceMetadata.sourceKey == "$normalizedSourceRoute:${fromVideoKey.bvid}"
    val sharedReadyVideoToSourceCard = sourceMetadata.sharedTransitionReady &&
        sourceMatchesCurrentVideo &&
        toKey != null &&
        isCardReturnTargetNavKey(toKey)
    if (cardTransitionEnabled && sharedReadyVideoToSourceCard) {
        return BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
    }
    if (!cardTransitionEnabled && sharedReadyVideoToSourceCard) {
        resolveCardDisabledReturnTransition(sourceMetadata.cardSourceDirection)?.let {
            return it
        }
    }
    return if (cardTransitionEnabled) {
        BiliPaiNavRouteTransition.CLASSIC_CARD
    } else {
        BiliPaiNavRouteTransition.FALLBACK
    }
}

internal fun shouldInterceptSystemBackForNavigation3(
    mode: BiliPaiNavMotionMode,
    appBackActionRequiresInterception: Boolean
): Boolean {
    if (appBackActionRequiresInterception) return true
    return mode == BiliPaiNavMotionMode.CLASSIC_CARD
}

private fun resolveCardDisabledReturnTransition(
    sourceDirection: BiliPaiNavCardSourceDirection
): BiliPaiNavRouteTransition? {
    return when (sourceDirection) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
        BiliPaiNavCardSourceDirection.NONE -> null
    }
}
