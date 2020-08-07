package ru.`object`.detection.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class CameraPermissionsResolver(
        private val activity: AppCompatActivity
) {
    private var onSuccessHandler: (() -> Unit)? = null
    private var onFailHandler: ((message: String) -> Unit)? = null

    private val requestCameraPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val isCameraPermissionGranted = result[Manifest.permission.CAMERA] ?: false

                if (isCameraPermissionGranted) {
                    onSuccessHandler?.invoke()
                } else {
                    onFailHandler?.invoke("Camera permission required")
                }
            }

    fun checkAndRequestPermissionsIfNeeded(
            onSuccess: () -> Unit,
            onFail: (message: String) -> Unit
    ) {
        onSuccessHandler = onSuccess
        onFailHandler = onFail

        if (isAllPermissionsGranted()) {
            onSuccess.invoke()
        } else {
            requestCameraPermissionLauncher.launch(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun isAllPermissionsGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.CAMERA)
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
}