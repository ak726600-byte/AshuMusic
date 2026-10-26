package com.metrolist.music.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.ProfilePhotoVersionKey
import com.metrolist.music.constants.StartupEnabledKey
import com.metrolist.music.constants.StartupGreetingSecondsKey
import com.metrolist.music.constants.StartupLogoSecondsKey
import com.metrolist.music.constants.StartupTitleSecondsKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val STAGE_LOGO = 0
private const val STAGE_TITLE = 1
private const val STAGE_GREETING = 2

/**
 * Start-up screen, played once each time the app is opened:
 * logo -> "Ashu Music" -> "Hey, <name> 👋" -> fade away.
 * Each part's length (in seconds) is set in Settings > Personalize; 0 skips that part.
 */
@Composable
fun StartupGreeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Survives screen rotation, but a fresh launch of the app starts with false again.
    var finished by rememberSaveable { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(-1) }
    var name by remember { mutableStateOf("") }
    var hasPhoto by remember { mutableStateOf(false) }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(450),
        label = "startupAlpha",
    )

    if (finished) return

    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[StartupEnabledKey] ?: true
        name = prefs[UserNameKey]?.trim().orEmpty()
        hasPhoto = (prefs[ProfilePhotoVersionKey] ?: 0L) > 0L

        if (enabled) {
            val logoMs = ((prefs[StartupLogoSecondsKey] ?: 1.0f) * 1000).toLong()
            val titleMs = ((prefs[StartupTitleSecondsKey] ?: 1.5f) * 1000).toLong()
            val greetMs = ((prefs[StartupGreetingSecondsKey] ?: 2.0f) * 1000).toLong()

            if (logoMs > 0L) {
                stage = STAGE_LOGO
                delay(logoMs)
            }
            if (titleMs > 0L) {
                stage = STAGE_TITLE
                delay(titleMs)
            }
            if (greetMs > 0L && name.isNotBlank()) {
                stage = STAGE_GREETING
                delay(greetMs)
            }
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
        Crossfade(targetState = stage, animationSpec = tween(400), label = "startupText") { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (s) {
                    STAGE_LOGO -> {
                        Image(
                            painter = painterResource(R.drawable.ashu_photo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(140.dp).clip(CircleShape),
                        )
                    }

                    STAGE_TITLE -> {
                        Text(
                            text = "Ashu Music",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    STAGE_GREETING -> {
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

                    else -> {}
                }
            }
        }
    }
}
