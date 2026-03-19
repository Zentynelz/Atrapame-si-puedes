package com.equipo.atrapame.presentation.game

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AffectiveManager(private val context: Context, private val lifecycleOwner: LifecycleOwner) {
    var currentSmilingProbability: Float = 0f
    var currentEyeOpenProbability: Float = 0f
    var currentAudioAmplitude: Int = 0

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isRecordingAudio = false
    private var audioRecord: AudioRecord? = null

    // Options for Face Detection for FACS analysis
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(faceDetectorOptions)

    fun start() {
        if (hasPermissions()) {
            startCamera()
            startAudioRecording()
        } else {
            Log.e("AffectiveManager", "Permissions not granted!")
        }
    }

    fun stop() {
        cameraExecutor.shutdown()
        isRecordingAudio = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                // Binding headless (no Preview) to just do Image Analysis
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalyzer)
                Log.d("AffectiveManager", "Camera bound to lifecycle for telemetry.")
            } catch (e: Exception) {
                Log.e("AffectiveManager", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        currentSmilingProbability = face.smilingProbability ?: 0f
                        currentEyeOpenProbability = face.rightEyeOpenProbability ?: 0f
                        Log.d("AffectiveTelemetry", "Smile: $currentSmilingProbability | Eye: $currentEyeOpenProbability")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AffectiveManager", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun startAudioRecording() {
        val sampleRate = 8000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecordingAudio = true

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isRecordingAudio) {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    var maxAmplitude = 0
                    for (i in 0 until readSize) {
                        val amp = Math.abs(buffer[i].toInt())
                        if (amp > maxAmplitude) {
                            maxAmplitude = amp
                        }
                    }
                    currentAudioAmplitude = maxAmplitude
                    Log.d("AffectiveTelemetry", "Mic Amplitude: $maxAmplitude")
                }
            }
        }.start()
        Log.d("AffectiveManager", "Audio recording started for telemetry.")
    }
}
