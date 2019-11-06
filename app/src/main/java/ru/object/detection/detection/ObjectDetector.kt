package ru.`object`.detection.detection

import android.content.res.AssetManager
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import ru.`object`.detection.util.DetectorUtils
import java.nio.ByteBuffer

class ObjectDetector constructor(
        assetManager: AssetManager,
        modelFilename: String,
        labelFilename: String,
        useNnapi: Boolean = false,
        numThreads: Int = DetectorUtils.NUM_THREADS,
        private val minimumConfidence: Float,
        private val numDetections: Int,
        private val inputSize: Int = 0,
        private val isModelQuantized: Boolean = false
) {

    private var labels: List<String> = emptyList()

    private val interpreter: Interpreter

    private val imgData: ByteBuffer

    init {
        labels = DetectorUtils.loadLabelsFile(assetManager, labelFilename)

        imgData = DetectorUtils.createImageBuffer(inputSize, isModelQuantized)

        interpreter = Interpreter(
                DetectorUtils.loadModelFile(assetManager, modelFilename),
                Interpreter.Options()
                        .setNumThreads(numThreads)
                        .setUseNNAPI(useNnapi)
        )
    }

    fun detect(pixels: IntArray): List<DetectionResult> {
        DetectorUtils.fillBuffer(
                imgData = imgData,
                pixels = pixels,
                inputSize = inputSize,
                isModelQuantized = isModelQuantized
        )

        val inputArray = arrayOf(imgData)
        val resultHolder = InterpreterResultHolder(numDetections)

        interpreter.runForMultipleInputsOutputs(inputArray, resultHolder.createOutputMap())

        return collectDetectionResult(resultHolder)
    }

    private fun collectDetectionResult(holder: InterpreterResultHolder): List<DetectionResult> {
        // SSD Mobilenet V1 Model assumes class 0 is background class
        // in label file and class labels start from 1 to number_of_classes+1,
        // while outputClasses correspond to class index from 0 to number_of_classes

        val labelOffset = 1

        val result = mutableListOf<DetectionResult>()

        for (i in 0 until holder.detections) {
            val confidence = holder.outputScores[0][i]
            if (confidence < minimumConfidence) continue

            val title = labels[holder.outputClasses[0][i].toInt() + labelOffset]

            val location = RectF(
                    holder.outputLocations[0][i][1] * inputSize,
                    holder.outputLocations[0][i][0] * inputSize,
                    holder.outputLocations[0][i][3] * inputSize,
                    holder.outputLocations[0][i][2] * inputSize
            )

            result.add(DetectionResult(id = i, title = title, confidence = confidence, location = location))
        }

        return result
    }
}