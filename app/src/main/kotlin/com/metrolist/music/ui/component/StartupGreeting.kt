package com.metrolist.music.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.ProfilePhotoVersionKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Remembered for the lifetime of the app process, so rotating the screen does not replay it.
private var startupGreetingShown = false

/**
 * Shown once at app start: "Ashu Play", then "Hey, <name> 👋" (if a name is set),
 * then fades away.
 */
@Composable
fun StartupGreeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var finished by remember { mutableStateOf(startupGreetingShown) }
    var fadeOut by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var hasPhoto by remember { mutableStateOf(false) }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(450),
        label = "startupAlpha",
    )

    if (finished) return

    LaunchedEffect(Unit) {
        startupGreetingShown = true
        val prefs = context.dataStore.data.first()
        name = prefs[UserNameKey]?.trim().orEmpty()
        hasPhoto = (prefs[ProfilePhotoVersionKey] ?: 0L) > 0L
        delay(1300)
        if (name.isNotBlank()) {
            stage = 1
            delay(1600)
        }
        fadeOut = true
        delay(500)
        finished = true
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .alpha(overlayAlpha)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Crossfade(targetState = stage, animationSpec = tween(500), label = "startupText") { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (s == 0) {
                        Text(
                            text = "Ashu Music",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        if (hasPhoto) {
                            ProfilePhoto(size = 110.dp)
                            Spacer(Modifier.height(20.dp))
                        }
                        Text(
                            text = "Hey, $name \uD83D\uDC4B",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}
