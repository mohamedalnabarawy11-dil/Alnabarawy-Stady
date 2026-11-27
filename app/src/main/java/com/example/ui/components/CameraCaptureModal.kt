package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.SoundEngine
import com.example.core.ImageUtils
import com.example.i18n.AppStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.LocalNabraawyColors
import com.example.ui.theme.ObsidianPure
import java.io.File

private const val TAG = "CameraCaptureModal"

@Composable
fun CameraCaptureModal(
    strings: AppStrings,
    soundEngine: SoundEngine,
    onCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalNabraawyColors.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianPure)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LuxurySectionCard(
                        title = strings.cameraPermissionTitle,
                        icon = Icons.Filled.CameraAlt
                    ) {
                        Text(
                            text = strings.cameraPermissionBody,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LuxuryGoldButton(
                                text = strings.cancelBtn,
                                onClick = onDismiss,
                                soundEngine = soundEngine,
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            LuxuryGoldButton(
                                text = strings.grantPermissionBtn,
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                soundEngine = soundEngine,
                                isPrimary = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                val imageCapture = remember {
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                }
                val previewView = remember {
                    PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Camera bind failed", e)
                            cameraError = e.localizedMessage
                        }
                    }, executor)

                    onDispose {
                        try {
                            cameraProviderFuture.get().unbindAll()
                        } catch (_: Exception) {
                        }
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Golden framing guide
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.86f)
                        .height(340.dp)
                        .border(1.5.dp, ChampagneGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                )

                // Top close bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            soundEngine.playClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianPure.copy(alpha = 0.75f))
                            .border(1.dp, ChampagneGold.copy(alpha = 0.6f), CircleShape)
                            .testTag("camera_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.cancelBtn,
                            tint = IvoryWhite
                        )
                    }
                }

                if (!cameraError.isNullOrBlank()) {
                    Text(
                        text = cameraError.orEmpty(),
                        color = IvoryWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(ObsidianPure.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )
                }

                // Bottom shutter button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LuxuryGoldButton(
                        text = strings.capturePhotoBtn,
                        icon = Icons.Filled.CameraAlt,
                        enabled = !isCapturing,
                        soundEngine = soundEngine,
                        testTagName = "camera_capture_button",
                        onClick = {
                            isCapturing = true
                            cameraError = null
                            val tempFile = File(context.cacheDir, "nabraawy_cam_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            val executor = ContextCompat.getMainExecutor(context)
                            imageCapture.takePicture(
                                outputOptions,
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        try {
                                            val bmp = ImageUtils.decodeFileToSafeBitmap(tempFile)
                                            if (bmp != null) {
                                                onCaptured(bmp)
                                            } else {
                                                cameraError = "Could not process captured photo."
                                                isCapturing = false
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Captured file decode failed", e)
                                            cameraError = e.localizedMessage
                                            isCapturing = false
                                        } finally {
                                            tempFile.delete()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e(TAG, "Capture error", exception)
                                        tempFile.delete()
                                        cameraError = exception.localizedMessage
                                        isCapturing = false
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
