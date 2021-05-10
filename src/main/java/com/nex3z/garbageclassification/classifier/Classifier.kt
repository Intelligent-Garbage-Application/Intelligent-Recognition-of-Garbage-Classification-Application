package com.nex3z.garbageclassification.classifier

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(context: Context) {

    private val options = with(Interpreter.Options()) {
        // setUseNNAPI(true)
        // setAllowFp16PrecisionForFp32(true)
        setNumThreads(5)
    }

    private val interpreter: Interpreter = Interpreter(loadModelFile(context), options)

    private val imageData: ByteBuffer = ByteBuffer.allocateDirect(
        4 * BATCH_SIZE * IMG_HEIGHT * IMG_WIDTH * NUM_CHANNEL
    ).apply { order(ByteOrder.nativeOrder()) }

    private val imagePixels = IntArray(IMG_HEIGHT * IMG_WIDTH)
    private val result = Array(1) { FloatArray(NUM_CLASSES) }

    fun classify(bitmap: Bitmap): Result {
        convertBitmapToByteBuffer(bitmap)
        val startTime = SystemClock.uptimeMillis()
        interpreter.run(imageData, result)
        val endTime = SystemClock.uptimeMillis()
        val timeCost = endTime - startTime
        Timber.v("classify(): timeCost = $timeCost")
        return Result(result[0])
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_NAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        imageData.rewind()

        bitmap.getPixels(
            imagePixels, 0, bitmap.width, 0, 0,
            bitmap.width, bitmap.height
        )

        for (pixel in 0 until IMG_HEIGHT * IMG_WIDTH) {
            val value = imagePixels[pixel]
            imageData.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            imageData.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            imageData.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
        }
    }

    companion object {
        private const val MODEL_NAME = "garbage.tflite"
        private const val BATCH_SIZE = 1
        const val IMG_HEIGHT = 224
        const val IMG_WIDTH = 224
        private const val NUM_CHANNEL = 3
        private const val NUM_CLASSES = 40
        private const val IMAGE_MEAN: Float = 127.5f
        private const val IMAGE_STD: Float = 127.5f
    }
}
