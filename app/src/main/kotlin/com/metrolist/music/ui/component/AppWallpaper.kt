package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.constants.WallpaperDimKey
import com.metrolist.music.constants.WallpaperVersionKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val WALLPAPER_FILE = "wallpaper.img"

fun wallpaperFile(context: Context): File = File(context.filesDir, WALLPAPER_FILE)

/** Copies the picked image into app storage. Returns true on success. */
fun saveWallpaper(context: Context, uri: Uri): Boolean =
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            wallpaperFile(context).outputStream().use { output -> input.copyTo(output) }
        } != null
    } catch (e: Exception) {
        false
    }

fun removeWallpaper(context: Context) {
    try {
        wallpaperFile(context).delete()
    } catch (e: Exception) {
        // ignore
    }
}

private fun loadWallpaper(context: Context): ImageBitmap? {
    val file = wallpaperFile(context)
    if (!file.exists()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 2000) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

/** Draws the user's wallpaper (if any) with a theme-coloured overlay so text stays readable. */
@Composable
fun AppWallpaper(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version by rememberPreference(WallpaperVersionKey, 0L)
    val dim by rememberPreference(WallpaperDimKey, 0.55f)
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(version) {
        bitmap =
            if (version > 0L) {
                withContext(Dispatchers.IO) { loadWallpaper(context) }
            } else {
                null
            }
    }

    val image = bitmap
    if (image != null) {
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
                    .background(MaterialTheme.colorScheme.background.copy(alpha = dim)),
            )
        }
    }
}
