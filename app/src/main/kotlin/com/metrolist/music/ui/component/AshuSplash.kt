package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.metrolist.music.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** False while the Ashu logo splash is playing; the start-up greeting waits for it to turn true. */
val LocalSplashDone = compositionLocalOf { true }

@Composable
fun AshuSplash(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    var finished by rememberSaveable { mutableStateOf(false) }
    if (finished) return

    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color.Black else Color.White
    val fg = if (dark) Color.White else Color.Black

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(700, easing = ease)) }
            launch { logoScale.animateTo(1f, tween(900, easing = ease)) }
        }
        kotlinx.coroutines.delay(600)
        screenAlpha.animateTo(0f, tween(400))
        finished = true
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f)
            .alpha(screenAlpha.value)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        WallpaperLayer(WallpaperKind.STARTUP, overlayColor = bg, fadeIn = true)
        Image(
            painter = painterResource(R.drawable.ashu_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(fg),
            modifier = Modifier
                .width(220.dp)
                .alpha(logoAlpha.value)
                .scale(logoScale.value),
        )
    }
}
