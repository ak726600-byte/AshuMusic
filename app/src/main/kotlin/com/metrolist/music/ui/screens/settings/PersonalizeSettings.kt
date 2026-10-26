package com.metrolist.music.ui.screens.settings

import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.constants.StartupGreetingSecondsKey
import com.metrolist.music.constants.StartupTitleSecondsKey
import com.metrolist.music.constants.StartupLogoSecondsKey
import com.metrolist.music.constants.StartupEnabledKey
import androidx.compose.material3.Switch
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerUseWallpaperKey
import com.metrolist.music.constants.ProfilePhotoVersionKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ProfilePhoto
import com.metrolist.music.ui.component.WallpaperKind
import com.metrolist.music.ui.component.deleteImage
import com.metrolist.music.ui.component.profilePhotoFile
import com.metrolist.music.ui.component.saveImageTo
import com.metrolist.music.ui.component.wallpaperFile
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun SecondsSlider(label: String, key: Preferences.Key<Float>, default: Float) {
    var seconds by rememberPreference(key, default)
    Text(
        text = if (seconds <= 0f) "$label: skipped" else "$label: ${"%.1f".format(seconds)} s",
        style = MaterialTheme.typography.labelLarge,
    )
    Slider(
        value = seconds,
        onValueChange = { seconds = Math.round(it * 2f) / 2f },
        valueRange = 0f..5f,
        steps = 9,
    )
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
}

/** Choose / change / remove buttons + overlay slider for one wallpaper kind. */
@Composable
private fun WallpaperControls(kind: WallpaperKind) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by rememberPreference(kind.versionKey, 0L)
    var dim by rememberPreference(kind.dimKey, kind.defaultDim)

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { saveImageTo(context, uri, wallpaperFile(context, kind)) }
                    if (ok) {
                        version = System.currentTimeMillis()
                        Toast.makeText(context, "Wallpaper set", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not load that image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { picker.launch("image/*") },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (version > 0L) "Change" else "Choose photo")
        }
        FilledTonalButton(
            onClick = {
                deleteImage(wallpaperFile(context, kind))
                version = 0L
                Toast.makeText(context, "Wallpaper removed", Toast.LENGTH_SHORT).show()
            },
            enabled = version > 0L,
            modifier = Modifier.weight(1f),
        ) {
            Text("Remove")
        }
    }

    if (version > 0L) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Overlay strength (higher = easier to read)",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = dim,
            onValueChange = { dim = it },
            valueRange = 0f..0.9f,
        )
    }
}

@Composable
private fun WallpaperCard(kind: WallpaperKind, title: String, description: String) {
    ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            SectionTitle(title, description)
            WallpaperControls(kind)
        }
    }
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowInsets = LocalPlayerAwareWindowInsets.current

    var startupEnabled by rememberPreference(StartupEnabledKey, true)
    var savedName by rememberPreference(UserNameKey, "")
    var nameInput by remember(savedName) { mutableStateOf(savedName) }

    var profileVersion by rememberPreference(ProfilePhotoVersionKey, 0L)
    val profilePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { saveImageTo(context, uri, profilePhotoFile(context)) }
                    if (ok) {
                        profileVersion = System.currentTimeMillis()
                        Toast.makeText(context, "Profile photo set", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not load that image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    var playerStyle by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    var playerUseWallpaper by rememberPreference(PlayerUseWallpaperKey, false)
    val playerMode =
        when {
            playerUseWallpaper -> 3
            playerStyle == PlayerBackgroundStyle.BLUR -> 2
            else -> 1
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(16.dp))

        // Start-up screen
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                SectionTitle(
                    "Start-up screen",
                    "Played every time you open the app: logo, then \"Ashu Music\", then \"Hey, <name>\". Set a part to 0 to skip it.",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Show start-up screen",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = startupEnabled, onCheckedChange = { startupEnabled = it })
                }
                if (startupEnabled) {
                    Spacer(Modifier.height(8.dp))
                    SecondsSlider("Logo", StartupLogoSecondsKey, 1.0f)
                    SecondsSlider("\"Ashu Music\" text", StartupTitleSecondsKey, 1.5f)
                    SecondsSlider("\"Hey, name\" greeting", StartupGreetingSecondsKey, 2.0f)
                    Text(
                        text = "Close and reopen the app to see the change.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Username
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                SectionTitle("Your name", "Shown as a greeting on the Home screen.")
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(30) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        savedName = nameInput.trim()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save name")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Profile photo (DP)
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                SectionTitle("Profile photo (DP)", "Your own picture, shown next to your name on the Home screen.")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(84.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (profileVersion > 0L) {
                                ProfilePhoto(size = 84.dp)
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { profilePicker.launch("image/*") }) {
                            Text(if (profileVersion > 0L) "Change" else "Choose photo")
                        }
                        FilledTonalButton(
                            onClick = {
                                deleteImage(profilePhotoFile(context))
                                profileVersion = 0L
                            },
                            enabled = profileVersion > 0L,
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        WallpaperCard(
            kind = WallpaperKind.APP,
            title = "App wallpaper",
            description = "Background for the whole app (Home, Library, Settings...).",
        )

        Spacer(Modifier.height(20.dp))

        WallpaperCard(
            kind = WallpaperKind.SEARCH,
            title = "Search wallpaper",
            description = "A different background just for the Search screens. If not set, the app wallpaper is used.",
        )

        Spacer(Modifier.height(20.dp))

        // Player
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                SectionTitle("Player background", "What you see behind the song while the full player is open.")

                ModeRow("Theme color (default)", playerMode == 1) {
                    playerStyle = PlayerBackgroundStyle.DEFAULT
                    playerUseWallpaper = false
                }
                ModeRow("Blurred song thumbnail", playerMode == 2) {
                    playerStyle = PlayerBackgroundStyle.BLUR
                    playerUseWallpaper = false
                }
                ModeRow("My own wallpaper", playerMode == 3) {
                    playerStyle = PlayerBackgroundStyle.BLUR
                    playerUseWallpaper = true
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Player wallpaper (used when \"My own wallpaper\" is selected)",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                WallpaperControls(WallpaperKind.PLAYER)
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text("Personalize") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
    )
}
