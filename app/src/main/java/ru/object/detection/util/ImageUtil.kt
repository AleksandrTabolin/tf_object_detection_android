package ru.`object`.detection.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.File
import kotlin.math.abs

object ImageUtil {

    // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
    // are normalized to eight bits.
    private const val kMaxChannelValue = 262143

    fun saveBitmap(context: Context, bitmap: Bitmap, filename: String) {
        val file = File(context.filesDir, "$filename.jpeg")
        if (file.exists()) {
            file.delete()
        }
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    fun storePixels(bitmap: Bitmap, array: IntArray) {
        bitmap.getPixels(array, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    fun getTransformMatrix(
            applyRotation: Int,
            srcWidth: Int, srcHeight: Int,
            dstWidth: Int, dstHeight: Int
    ): Matrix {
        val matrix = Matrix()

        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)
            matrix.postRotate(applyRotation.toFloat())
        }

        val transpose = (abs(applyRotation) + 90) % 180 == 0

        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()

            matrix.postScale(scaleFactorX, scaleFactorY)
        }

        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }

        return matrix
    }


    fun convertYuvToRgb(
            imageProxy: ImageProxy,
            output: IntArray
    ) {
        val yData = imageProxy.planes[0].buffer
        val uData = imageProxy.planes[1].buffer
        val vData = imageProxy.planes[2].buffer

        val yRowStride = imageProxy.planes[0].rowStride
        val uvRowStride = imageProxy.planes[1].rowStride
        val uvPixelStride = imageProxy.planes[1].pixelStride

        var yp = 0
        for (j in 0 until imageProxy.height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)

            for (i in 0 until imageProxy.width) {
                val uvOffset = pUV + (i shr 1) * uvPixelStride

                output[yp++] = yuvToRgb(
                        y = 0xff and yData[pY + i].toInt(),
                        u = 0xff and uData[uvOffset].toInt(),
                        v = 0xff and vData[uvOffset].toInt()
                )
            }
        }
    }

    private fun yuvToRgb(y: Int, u: Int, v: Int): Int {
        // Adjust and check YUV values
        val y = if (y - 16 < 0) 0 else y - 16
        val u = u - 128
        val v = v - 128

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        val y1192 = 1192 * y
        var r = y1192 + 1634 * v
        var g = y1192 - 833 * v - 400 * u
        var b = y1192 + 2066 * u

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = if (r > kMaxChannelValue) kMaxChannelValue else if (r < 0) 0 else r
        g = if (g > kMaxChannelValue) kMaxChannelValue else if (g < 0) 0 else g
        b = if (b > kMaxChannelValue) kMaxChannelValue else if (b < 0) 0 else b

        return -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
    }

}