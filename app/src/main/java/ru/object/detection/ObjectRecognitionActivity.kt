package ru.`object`.detection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_object_recognition.*
import org.tensorflow.lite.examples.detection.R
import ru.`object`.detection.camera.CameraPermissionsResolver
import ru.`object`.detection.camera.ObjectDetectorAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectRecognitionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

    private lateinit var executor: ExecutorService

    private val cameraPermissionsResolver = CameraPermissionsResolver(this)

    private val objectDetectorConfig = ObjectDetectorAnalyzer.Config(
            minimumConfidence = 0.5f,
            numDetection = 10,
            inputSize = 300,
            isQuantized = true,
            modelFile = "detect.tflite",
            labelsFile = "labelmap.txt"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_recognition)
        executor = Executors.newSingleThreadExecutor()

        previewView = findViewById(R.id.preview_view)

        cameraPermissionsResolver.checkAndRequestPermissionsIfNeeded(
                onSuccess = {
                    getProcessCameraProvider(::bindCamera)
                },
                onFail = ::showSnackbar
        )
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

        val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageAnalysis.setAnalyzer(
                executor,
                ObjectDetectorAnalyzer(applicationContext, objectDetectorConfig, ::onDetectionResult)
        )

        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalysis,
                preview
        )

        preview.setSurfaceProvider(previewView.createSurfaceProvider())
    }

    private fun getProcessCameraProvider(onDone: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
                Runnable { onDone.invoke(cameraProviderFuture.get()) },
                ContextCompat.getMainExecutor(this)
        )
    }

    private fun onDetectionResult(result: ObjectDetectorAnalyzer.Result) {
        result_overlay.updateResults(result)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(root_container, message, Snackbar.LENGTH_LONG).show()
    }

}