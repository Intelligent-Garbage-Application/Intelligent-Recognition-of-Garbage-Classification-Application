package com.nex3z.garbageclassification.ui.camera

import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.nex3z.garbageclassification.classifier.Classifier
import com.nex3z.garbageclassification.ui.camera.model.Garbage
import com.nex3z.garbageclassification.ui.camera.model.Category
import com.nex3z.garbageclassification.util.convertYuv420ToArgb8888Bitmap
import com.nex3z.garbageclassification.util.preprocess
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClassifierViewModel(
    application: Application
) : AndroidViewModel(application) {

    val resultData: MutableLiveData<List<Garbage>> = MutableLiveData()
    val inputData: MutableLiveData<Bitmap> = MutableLiveData()

    private val classifier: Classifier
    private var lastAnalyzedTimestamp = 0L

    init {
        try {
            classifier = Classifier(application)
            Timber.i("init(): Classifier initialized")
        } catch (e: IOException) {
            Timber.e(e, "init(): Failed to init classifier")
            throw e
        }
    }

    fun classify(image: ImageProxy, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp <= ANALYSIS_INTERVAL) {
            return
        }
        Timber.v("classify(): format = ${image.format}, rotationDegrees = $rotationDegrees")

        val bitmap = convertYuv420ToArgb8888Bitmap(image)
        Timber.v("classify(): bitmap width = ${bitmap.width}, height = ${bitmap.height}")

        val scaled = preprocess(
            bitmap, Classifier.IMG_WIDTH, Classifier.IMG_HEIGHT, rotationDegrees)
        Timber.v("classify(): scaled width = ${scaled.width}, height = ${scaled.height}")
        inputData.postValue(scaled)

        val result = classifier.classify(scaled)
        Timber.v("classify(): result = $result")

        val results = result.results.map {
            Garbage(
                label = it.first,
                category = labelToType(it.first),
                probability = it.second
            )
        }
        resultData.postValue(results)

        lastAnalyzedTimestamp = currentTimestamp
    }

    companion object {
        private val ANALYSIS_INTERVAL = TimeUnit.SECONDS.toMillis(1)

        private fun labelToType(label: Int): Category {
            return when (label) {
                in 0..5 -> Category.RESIDUAL
                in 6..13 -> Category.HOUSEHOLD_FOOD
                in 14..36 -> Category.RECYCLABLE
                in 37..39 -> Category.HAZARDOUS
                else -> throw IllegalArgumentException("label out of range")
            }
        }
    }
}
