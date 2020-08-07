package ru.`object`.detection.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ru.`object`.detection.detection.DetectionResult
import ru.`object`.detection.detection.ObjectDetector
import ru.`object`.detection.util.ImageUtil
import ru.`object`.detection.util.YuvToRgbConverter
import java.util.concurrent.atomic.AtomicInteger

class ObjectDetectorAnalyzer(
        private val context: Context,
        private val config: Config,
        private val onDetectionResult: (Result) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ObjectDetectorAnalyzer"
        private val DEBUG = false
    }

    private val iterationCounter = AtomicInteger(0)

    private val debugHelper = DebugHelper(
            saveResult = false,
            context = context,
            resultHeight = config.inputSize,
            resultWidth = config.inputSize
    )

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    private val uiHandler = Handler(Looper.getMainLooper())

    private var inputArray = IntArray(config.inputSize * config.inputSize)

    private var objectDetector: ObjectDetector? = null

    private var rgbBitmap: Bitmap? = null
    private var resizedBitmap = Bitmap.createBitmap(config.inputSize, config.inputSize, Bitmap.Config.ARGB_8888)

    private var matrixToInput: Matrix? = null

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees

        val iteration = iterationCounter.getAndIncrement()

        val rgbBitmap = getArgbBitmap(image.width, image.height)

        yuvToRgbConverter.yuvToRgb(image, rgbBitmap)

        val transformation = getTransformation(rotationDegrees, image.width, image.height)

        image.close()

        Canvas(resizedBitmap).drawBitmap(rgbBitmap, transformation, null)

        ImageUtil.storePixels(resizedBitmap, inputArray)

        val objects = detect(inputArray)

        if (DEBUG) {
            debugHelper.saveResult(iteration, resizedBitmap, objects)
        }

        Log.d(TAG, "detection objects($iteration): $objects")

        val result = Result(
                objects = objects,
                imageWidth = config.inputSize,
                imageHeight = config.inputSize,
                imageRotationDegrees = rotationDegrees
        )



        uiHandler.post {
            onDetectionResult.invoke(result)
        }
    }

    private fun getTransformation(rotationDegrees: Int, srcWidth: Int, srcHeight: Int): Matrix {
        var toInput = matrixToInput
        if (toInput == null) {
            toInput = ImageUtil.getTransformMatrix(rotationDegrees, srcWidth, srcHeight, config.inputSize, config.inputSize)
            matrixToInput = toInput
        }
        return toInput
    }

    private fun getArgbBitmap(width: Int, height: Int): Bitmap {
        var bitmap = rgbBitmap
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) as Bitmap
            rgbBitmap = bitmap
        }
        return bitmap
    }

    private fun detect(inputArray: IntArray): List<DetectionResult> {
        var detector = objectDetector
        if (detector == null) {
            detector = ObjectDetector(
                    assetManager = context.assets,
                    isModelQuantized = config.isQuantized,
                    inputSize = config.inputSize,
                    labelFilename = config.labelsFile,
                    modelFilename = config.modelFile,
                    numDetections = config.numDetection,
                    minimumConfidence = config.minimumConfidence,
                    numThreads = 1,
                    useNnapi = false
            )
            objectDetector = detector
        }

        return detector.detect(inputArray)
    }

    data class Config(
            val minimumConfidence: Float,
            val numDetection: Int,
            val inputSize: Int,
            val isQuantized: Boolean,
            val modelFile: String,
            val labelsFile: String
    )

    data class Result(
            val objects: List<DetectionResult>,
            val imageWidth: Int,
            val imageHeight: Int,
            val imageRotationDegrees: Int
    )
}