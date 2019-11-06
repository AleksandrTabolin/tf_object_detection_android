package ru.`object`.detection.util

import android.content.res.AssetManager
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object DetectorUtils {

    private const val IMAGE_MEAN = 128.0f
    private const val IMAGE_STD = 128.0f
    const val NUM_THREADS = 4

    fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun loadLabelsFile(assets: AssetManager, labelFilename: String): List<String> {
        return assets.open(labelFilename).bufferedReader().use { it.readLines() }
    }

    fun createImageBuffer(inputSize: Int, isModelQuantized: Boolean): ByteBuffer {
        val numBytesPerChannel: Int = if (isModelQuantized) 1 else 4
        val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * numBytesPerChannel)
        buffer.order(ByteOrder.nativeOrder())
        return buffer
    }

    fun fillBuffer(
            imgData: ByteBuffer,
            pixels: IntArray,
            inputSize: Int,
            isModelQuantized: Boolean
    ) {
        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = pixels[i * inputSize + j]
                if (isModelQuantized) {
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else {
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
    }

}