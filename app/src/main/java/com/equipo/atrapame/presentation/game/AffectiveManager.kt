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

// TarsosDSP (solo core — sin AudioDispatcherFactory)
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import be.tarsos.dsp.io.TarsosDSPAudioFormat

// Silero VAD
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

class AffectiveManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    // ─── Datos de rostro ────────────────────────────────────────────
    var currentSmilingProbability: Float = 0f
    var currentEyeOpenProbability: Float = 0f

    // ─── Datos de voz mejorados ─────────────────────────────────────
    var currentAudioAmplitude: Int = 0
    var currentPitchHz: Float = 0f
    var currentPitchConfidence: Float = 0f
    var isSpeechDetected: Boolean = false

    // Métricas derivadas para estrés
    var pitchVariability: Float = 0f
    private val recentPitches = mutableListOf<Float>()
    private val MAX_PITCH_HISTORY = 30

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isRecordingAudio = false
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    private var vadDetector: VadSilero? = null

    // Face detection
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val detector = FaceDetection.getClient(faceDetectorOptions)

    fun start() {
        if (hasPermissions()) {
            startCamera()
            startVoiceAnalysis()
        } else {
            Log.e("AffectiveManager", "Permissions not granted!")
        }
    }

    fun stop() {
        cameraExecutor.shutdown()
        isRecordingAudio = false
        audioThread?.interrupt()
        audioThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        vadDetector?.close()
        vadDetector = null
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    // ─── Análisis de voz con TarsosDSP (core) + Silero VAD ─────────

    private fun startVoiceAnalysis() {
        val sampleRate = 16000
        val bufferSize = 1024  // TarsosDSP YIN necesita potencia de 2

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, bufferSize * 2)
        )

        // Inicializar Silero VAD
        vadDetector = VadSilero(
            context,
            SampleRate.SAMPLE_RATE_16K,
            FrameSize.FRAME_SIZE_512,
            Mode.NORMAL,
            silenceDurationMs = 300,
            speechDurationMs = 50
        )

        // Crear PitchProcessor de TarsosDSP (funciona sin AudioDispatcher)
        val pitchProcessor = PitchProcessor(
            PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize
        ) { result: PitchDetectionResult, _: AudioEvent ->
            val pitchHz = result.pitch
            val probability = result.probability

            if (pitchHz > 0 && probability > 0.85f) {
                currentPitchHz = pitchHz
                currentPitchConfidence = probability

                synchronized(recentPitches) {
                    recentPitches.add(pitchHz)
                    if (recentPitches.size > MAX_PITCH_HISTORY) {
                        recentPitches.removeAt(0)
                    }
                    pitchVariability = calculatePitchVariability()
                }
            }
        }

        audioRecord?.startRecording()
        isRecordingAudio = true

        audioThread = Thread {
            val shortBuffer = ShortArray(bufferSize)
            val floatBuffer = FloatArray(bufferSize)
            val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)

            while (isRecordingAudio) {
                val readSize = audioRecord?.read(shortBuffer, 0, bufferSize) ?: 0
                if (readSize <= 0) continue

                // Convertir Short → Float normalizado [-1.0, 1.0]
                for (i in 0 until readSize) {
                    floatBuffer[i] = shortBuffer[i] / 32768f
                }

                // 1) RMS
                var sumSquares = 0.0
                for (i in 0 until readSize) {
                    sumSquares += floatBuffer[i] * floatBuffer[i]
                }
                val rms = Math.sqrt(sumSquares / readSize)
                currentAudioAmplitude = (rms * 300).coerceIn(0.0, 100.0).toInt()

                // 2) Pitch con TarsosDSP — alimentar manualmente
                val audioEvent =
                    AudioEvent(format)
                audioEvent.floatBuffer = floatBuffer
                pitchProcessor.process(audioEvent)

                // 3) VAD con Silero
                val vadBuffer = ShortArray(readSize)
                System.arraycopy(shortBuffer, 0, vadBuffer, 0, readSize)
                isSpeechDetected = try {
                    vadDetector?.isSpeech(vadBuffer) ?: false
                } catch (e: Exception) {
                    false
                }

                Log.d("AffectiveTelemetry",
                    "RMS: $currentAudioAmplitude | Pitch: $currentPitchHz Hz " +
                            "| Variability: $pitchVariability | Speech: $isSpeechDetected")
            }
        }.apply {
            name = "AudioAnalysis"
            start()
        }

        Log.d("AffectiveManager", "Voice analysis started with TarsosDSP + Silero VAD")
    }

    private fun calculatePitchVariability(): Float {
        if (recentPitches.size < 5) return 0f
        val mean = recentPitches.average().toFloat()
        val variance = recentPitches.map { (it - mean) * (it - mean) }.average().toFloat()
        return Math.sqrt(variance.toDouble()).toFloat()
    }

    // ─── Cámara ─────────────────────────────────────────────────────

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
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("AffectiveManager", "Camera binding failed", e)
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
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AffectiveManager", "Face detection failed", e)
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}