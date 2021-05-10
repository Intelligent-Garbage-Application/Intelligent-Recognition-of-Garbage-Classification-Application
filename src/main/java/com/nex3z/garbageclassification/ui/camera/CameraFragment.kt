package com.nex3z.garbageclassification.ui.camera

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.nex3z.garbageclassification.R
import com.nex3z.garbageclassification.ui.camera.model.Garbage
import com.nex3z.garbageclassification.ui.camera.model.Category
import kotlinx.android.synthetic.main.camera_fragment.*
import timber.log.Timber

class CameraFragment : Fragment() {

    private lateinit var viewModel: ClassifierViewModel
    private lateinit var labels: Array<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ClassifierViewModel::class.java)
        init()
    }

    private fun init() {
        labels = resources.getStringArray(R.array.labels)
        initView()
        bindData()
        texture_view_finder.post { initCamera() }
    }

    private fun initView() {
        texture_view_finder.setOnLongClickListener {
            val showDetail = !tv_detailed.isVisible
            Timber.v("initView(): showDetail = $showDetail")
            tv_detailed.isVisible = showDetail
            iv_category.isVisible = !showDetail
            tv_result.isVisible = !showDetail
            tv_dispose_requirement.isVisible = !showDetail
            true
        }
    }

    private fun initCamera() {
        val metrics = DisplayMetrics().also { texture_view_finder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        Timber.d("initCamera(): metrics = $metrics, screenAspectRatio = $screenAspectRatio")

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(texture_view_finder.display.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = texture_view_finder.parent as ViewGroup
            parent.removeView(texture_view_finder)
            parent.addView(texture_view_finder, 0)
            texture_view_finder.surfaceTexture = it.surfaceTexture
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setTargetResolution(Size(224, 224))
            val analyzerThread = HandlerThread("ImageAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = ImageAnalysis.Analyzer { image, rotationDegrees ->
                image?.let { viewModel.classify(it, rotationDegrees) }
            }
        }

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun bindData() {
        viewModel.inputData.observe(this, Observer<Bitmap> {
            if (iv_processed.isVisible) {
                iv_processed.setImageBitmap(it)
            }
        })
        viewModel.resultData.observe(this, Observer<List<Garbage>> { renderResult(it) })
    }

    private fun renderResult(result: List<Garbage>) {
        val garbage = result[0]
        if (garbage.probability >= PROBABILITY_THRESHOLD) {
            tv_result.text = labels[garbage.label]
            when (garbage.category) {
                Category.RECYCLABLE -> {
                    iv_category.setImageResource(R.drawable.ic_recyclable)
                    tv_dispose_requirement.setText(R.string.m_req_recyclable)
                }
                Category.HAZARDOUS -> {
                    iv_category.setImageResource(R.drawable.ic_hazardous)
                    tv_dispose_requirement.setText(R.string.m_req_hazardous)
                }
                Category.HOUSEHOLD_FOOD -> {
                    iv_category.setImageResource(R.drawable.ic_household_food)
                    tv_dispose_requirement.setText(R.string.m_req_household_food)
                }
                Category.RESIDUAL -> {
                    iv_category.setImageResource(R.drawable.ic_residual)
                    tv_dispose_requirement.setText(R.string.m_req_residual)
                }
            }
        } else {
            iv_category.setImageResource(R.drawable.ic_unknown)
            tv_result.setText(R.string.c_unknown)
            tv_dispose_requirement.text = ""
        }

        val texts = result
            .take(5)
            .map { String.format(getString(R.string.c_result), it.probability, labels[it.label]) }
        tv_detailed.text = texts.joinToString("\n")
    }

    companion object {
        private const val PROBABILITY_THRESHOLD: Float = 0.8f
    }
}
