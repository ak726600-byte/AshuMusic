package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.constants.PlayerWallpaperDimKey
import com.metrolist.music.constants.PlayerWallpaperVersionKey
import com.metrolist.music.constants.ProfilePhotoVersionKey
import com.metrolist.music.constants.SearchWallpaperDimKey
import com.metrolist.music.constants.SearchWallpaperVersionKey
import com.metrolist.music.constants.WallpaperDimKey
import com.metrolist.music.constants.WallpaperVersionKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The three places a custom wallpaper can be used. */
enum class WallpaperKind(
    val fileName: String,
    val versionKey: Preferences.Key<Long>,
    val dimKey: Preferences.Key<Float>,
    val defaultDim: Float,
) {
    APP("wallpaper.img", WallpaperVersionKey, WallpaperDimKey, 0.55f),
    SEARCH("wallpaper_search.img", SearchWallpaperVersionKey, SearchWallpaperDimKey, 0.55f),
    PLAYER("wallpaper_player.img", PlayerWallpaperVersionKey, PlayerWallpaperDimKey, 0.35f),
}

fun wallpaperFile(context: Context, kind: WallpaperKind): File = File(context.filesDir, kind.fileName)

fun profilePhotoFile(context: Context): File = File(context.filesDir, "profile.img")

/** Copies the picked image into app storage. Returns true on success. */
fun saveImageTo(context: Context, uri: Uri, target: File): Boolean =
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } != null
    } catch (e: Exception) {
        false
    }

fun deleteImage(file: File) {
    try {
        file.delete()
    } catch (e: Exception) {
        // ignore
    }
}

private fun loadBitmap(file: File, maxSidePx: Int): ImageBitmap? {
    if (!file.exists()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > maxSidePx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

/**
 * Draws the user's wallpaper of the given kind (if one is set) with an overlay
 * so text stays readable. Draws nothing when no wallpaper is set.
 */
@Composable
fun WallpaperLayer(
    kind: WallpaperKind,
    modifier: Modifier = Modifier,
    overlayColor: Color? = null,
) {
    val context = LocalContext.current
    val version by rememberPreference(kind.versionKey, 0L)
    val dim by rememberPreference(kind.dimKey, kind.defaultDim)
    var bitmap by remember(kind) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(kind, version) {
        bitmap =
            if (version > 0L) {
                withContext(Dispatchers.IO) { loadBitmap(wallpaperFile(context, kind), 2000) }
            } else {
                null
            }
    }

    val image = bitmap
    if (image != null) {
        val overlay = overlayColor ?: MaterialTheme.colorScheme.background
        Box(modifier.fillMaxSize()) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(overlay.copy(alpha = dim)),
            )
        }
    }
}

/** The user's own profile picture (DP), drawn as a circle. Draws nothing if none is set. */
@Composable
fun ProfilePhoto(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version by rememberPreference(ProfilePhotoVersionKey, 0L)
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(version) {
        bitmap =
            if (version > 0L) {
                withContext(Dispatchers.IO) { loadBitmap(profilePhotoFile(context), 512) }
            } else {
                null
            }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    }
}
