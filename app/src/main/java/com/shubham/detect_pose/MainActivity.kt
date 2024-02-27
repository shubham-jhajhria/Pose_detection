package com.shubham.detect_pose


import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private val poseResult = mutableStateOf<ResultBundle?>(null)
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var executor: Executor
    private val poseLandmarkerHelperListener=MyLandmarkerListener()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        poseLandmarker = initializePoseLandmarker()
        executor = Executors.newSingleThreadExecutor()

        // Set content using Compose
        setContent {
            ani()
            if( BasicCountdownTimer()==0){
                PoseDetectionScreen(poseLandmarker, executor,poseResult)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        finish()
    }

    private fun initializePoseLandmarker(): PoseLandmarker {
        val modelName = "pose_landmarker_lite.task"
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath(modelName)

        val optionsBuilder =
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinPoseDetectionConfidence(0.5F)
                .setMinTrackingConfidence(0.5F)
                .setMinPosePresenceConfidence(0.5F)
                .setNumPoses(1)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .setRunningMode(RunningMode.LIVE_STREAM)
        val options = optionsBuilder.build()
        return PoseLandmarker.createFromOptions(this, options)
    }
    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        poseResult.value = ResultBundle(
            listOf(result),
            inferenceTime,
            input.height,
            input.width
        )

        poseLandmarkerHelperListener.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    // Return errors thrown during detection to this PoseLandmarkerHelper's
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener.onError(
            error.message ?: "An unknown error has occurred"
        )
    }
    companion object {
        const val OTHER_ERROR = 0

    }
    data class ResultBundle(
        val results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }

}

class MyLandmarkerListener : MainActivity.LandmarkerListener {
    override fun onError(error: String, errorCode: Int) {
        // Handle error
        Log.e("MyLandmarkerListener", "Error occurred: $error, Code: $errorCode")
    }

    override fun onResults(resultBundle: MainActivity.ResultBundle) {

        Log.d("MyLandmarkerListener", "Received results: $resultBundle")
    }

}