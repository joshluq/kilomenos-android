package es.joshluq.kmsafe.ui.common.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import es.joshluq.canvaskit.components.buttons.CanvasKitButton
import es.joshluq.canvaskit.components.navigation.CanvasKitTopBar
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.core.ui.util.safeClick
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropImageScreen(
    uri: String,
    onCropSuccess: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var triggerCrop by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        runCatching {
            val inputUri = uri.toUri()
            val inputStream = context.contentResolver.openInputStream(inputUri)
            bitmap = BitmapFactory.decodeStream(inputStream)
        }.onFailure {
            Log.e("CropImageScreen", "Failed to load bitmap from URI: $uri", it)
        }
    }

    Scaffold(
        topBar = {
            CanvasKitTopBar(
                title = { Text("Ajustar Foto", style = CanvasKitTheme.typography.bodyLarge) },
                navigationIcon = {
                    IconButton(onClick = safeClick { onCancel() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.acc_back),
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { b ->
                val imageBitmap = remember(b) { b.asImageBitmap() }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ImageCropper(
                            modifier = Modifier.fillMaxSize(),
                            imageBitmap = imageBitmap,
                            contentDescription = stringResource(R.string.acc_crop_image),
                            crop = triggerCrop,
                            cropProperties = CropDefaults.properties(
                                cropOutlineProperty = CropOutlineProperty(
                                    OutlineType.Rect,
                                    RectCropShape(0, "Rect")
                                ),
                                aspectRatio = AspectRatio(2.5f / 1f),
                                fixedAspectRatio = true,
                                handleSize = 30f
                            ),
                            onCropStart = {
                                Log.d("CropImageScreen", "Crop started...")
                            },
                            onCropSuccess = { cropped: ImageBitmap ->
                                Log.d("CropImageScreen", "Crop success!")
                                if (!isSaving) {
                                    isSaving = true
                                    val croppedAndroidBitmap = cropped.asAndroidBitmap()
                                    val resultUri = saveBitmapToCache(context, croppedAndroidBitmap)
                                    if (resultUri != null) {
                                        onCropSuccess(resultUri)
                                    } else {
                                        Log.e("CropImageScreen", "Failed to save cropped bitmap")
                                        isSaving = false
                                        triggerCrop = false
                                    }
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        CanvasKitButton(
                            onClick = {
                                if (!isSaving) {
                                    Log.d("CropImageScreen", "Apply Crop clicked")
                                    triggerCrop = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving && !triggerCrop,
                            loading = isSaving
                        ) { contentColor ->
                            Text(stringResource(R.string.onboarding_crop_button), color = contentColor)
                        }
                    }
                }
            } ?: CircularProgressIndicator(color = CanvasKitTheme.colors.brandAccent)

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CanvasKitTheme.colors.brandAccent)
                }
            }
        }
    }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.cacheDir, "cropped_vehicle_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        Log.e("CropImageScreen", "Failed to save cropped bitmap to file", e)
        null
    }
}
