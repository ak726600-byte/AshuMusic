package com.metrolist.music.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.constants.WallpaperDimKey
import com.metrolist.music.constants.WallpaperVersionKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.removeWallpaper
import com.metrolist.music.ui.component.saveWallpaper
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowInsets = LocalPlayerAwareWindowInsets.current

    var savedName by rememberPreference(UserNameKey, "")
    var nameInput by remember(savedName) { mutableStateOf(savedName) }
    var wallpaperVersion by rememberPreference(WallpaperVersionKey, 0L)
    var dim by rememberPreference(WallpaperDimKey, 0.55f)

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { saveWallpaper(context, uri) }
                    if (ok) {
                        wallpaperVersion = System.currentTimeMillis()
                        Toast.makeText(context, "Wallpaper set", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not load that image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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

        // Username
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "Your name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Shown as a greeting on the Home screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
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

        // Wallpaper
        ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "App wallpaper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pick any photo from your gallery as the app background. Change or remove it any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (wallpaperVersion > 0L) "Change" else "Choose wallpaper")
                    }
                    FilledTonalButton(
                        onClick = {
                            removeWallpaper(context)
                            wallpaperVersion = 0L
                            Toast.makeText(context, "Wallpaper removed", Toast.LENGTH_SHORT).show()
                        },
                        enabled = wallpaperVersion > 0L,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Remove")
                    }
                }

                if (wallpaperVersion > 0L) {
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
