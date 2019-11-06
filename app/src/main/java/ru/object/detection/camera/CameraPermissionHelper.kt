package ru.`object`.detection.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import org.tensorflow.lite.examples.detection.R

class CameraPermissionHelper(
        private val activity: FragmentActivity
) {

    companion object {
        private const val PERMISSION_REQUEST_CAMERA = 1234
    }

    fun doIfHaveCameraPermission(block: () -> Unit) {
        if (hasCameraPermission()) {
            block.invoke()
        }
    }

    fun requestCameraPermissionIfDontHas(onNoPermission: () -> Unit) {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
            onNoPermission.invoke()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, onHasPermission: () -> Unit) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            val indexOfCamera = permissions.indexOf(Manifest.permission.CAMERA)

            if (grantResults.getOrNull(indexOfCamera) == PackageManager.PERMISSION_GRANTED) {
                onHasPermission.invoke()
            } else {
                Snackbar
                        .make(
                                activity.findViewById(android.R.id.content),
                                R.string.request_permission,
                                Snackbar.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}