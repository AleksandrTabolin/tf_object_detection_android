package ru.`object`.detection

import android.os.Bundle
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import kotlinx.android.synthetic.main.activity_object_recognition.*
import org.tensorflow.lite.examples.detection.R
import ru.`object`.detection.camera.CameraPermissionHelper
import ru.`object`.detection.camera.ObjectDetectorAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectRecognitionActivity : AppCompatActivity() {

    private lateinit var cameraPreview: TextureView

    private lateinit var executor: ExecutorService

    private val objectDetectorConfig = ObjectDetectorAnalyzer.Config(
            minimumConfidence = 0.5f,
            numDetection = 10,
            inputSize = 300,
            isQuantized = true,
            modelFile = "detect.tflite",
            labelsFile = "labelmap.txt"
    )

    private val cameraPermissionHelper = CameraPermissionHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_recognition)
        executor = Executors.newSingleThreadExecutor()

        cameraPreview = findViewById(R.id.camera_view)

        cameraPermissionHelper.doIfHaveCameraPermission(::startCamera)
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    override fun onStart() {
        cameraPermissionHelper.requestCameraPermissionIfDontHas(onNoPermission = CameraX::unbindAll)
        super.onStart()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults, onHasPermission = ::startCamera)
    }

    private fun startCamera() {
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, ObjectDetectorAnalyzer(applicationContext, objectDetectorConfig, ::onDetectionResult))
        }

        val previewConfig = PreviewConfig.Builder()
                .setLensFacing(CameraX.LensFacing.BACK)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener { previewOutput ->
            cameraPreview.surfaceTexture = previewOutput.surfaceTexture
        }

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun onDetectionResult(result: ObjectDetectorAnalyzer.Result) {
        result_overlay.updateResults(result)
    }

}