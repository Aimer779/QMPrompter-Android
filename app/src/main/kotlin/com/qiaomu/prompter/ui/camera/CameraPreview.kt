package com.qiaomu.prompter.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

enum class CameraPermissionState {
    Checking,
    Authorized,
    Denied,
    Unavailable
}

@Composable
fun CameraPreview(
    lensFacing: Int,
    onPermissionStateChange: (CameraPermissionState) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                CameraPermissionState.Authorized
            } else {
                CameraPermissionState.Checking
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState = if (granted) CameraPermissionState.Authorized else CameraPermissionState.Denied
    }

    LaunchedEffect(Unit) {
        if (permissionState == CameraPermissionState.Checking) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(permissionState) {
        onPermissionStateChange(permissionState)
    }

    AndroidView(
        modifier = modifier,
        factory = { factoryContext ->
            PreviewView(factoryContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            if (
                permissionState != CameraPermissionState.Authorized &&
                permissionState != CameraPermissionState.Unavailable
            ) {
                return@AndroidView
            }

            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    if (!provider.hasCamera(selector)) {
                        permissionState = CameraPermissionState.Unavailable
                        onCameraUnavailable()
                        return@addListener
                    }

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                    permissionState = CameraPermissionState.Authorized
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    )
}
