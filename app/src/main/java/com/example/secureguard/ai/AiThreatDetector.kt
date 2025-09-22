// FILE: app/src/main/java/com/example/secureguard/ai/AiThreatDetector.kt

package com.example.secureguard.ai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class AiThreatDetector(context: Context, modelName: String = "malware_model.tflite") {

    private var interpreter: Interpreter? = null
    private val inputSize: Int

    init {
        var tempInputSize = 0
        try {
            val modelBuffer = loadModelFile(context, modelName)
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)

            // Get the size of the input layer from the model itself
            tempInputSize = interpreter?.getInputTensor(0)?.shape()?.get(1) ?: 0

            Log.d("AiThreatDetector", "TensorFlow Lite model loaded successfully. Input size: $tempInputSize")
        } catch (e: Exception) {
            Log.e("AiThreatDetector", "Error loading TensorFlow Lite model: ${e.message}")
        }
        inputSize = tempInputSize
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getRiskScore(featureVector: FloatArray): Float {
        if (interpreter == null || inputSize == 0) {
            Log.e("AiThreatDetector", "Interpreter is not initialized or input size is zero.")
            return 0.0f
        }

        // Ensure the feature vector from the app matches the model's expected input size
        if (featureVector.size != inputSize) {
            Log.e("AiThreatDetector", "Input feature vector size (${featureVector.size}) does not match model's expected input size ($inputSize).")
            return 0.0f
        }

        // Create the input buffer with the correct size
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().put(featureVector)
        }

        // The output buffer expects a single float value (the risk score)
        val outputBuffer = ByteBuffer.allocateDirect(4 * 1).apply {
            order(ByteOrder.nativeOrder())
        }

        try {
            interpreter?.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e("AiThreatDetector", "Error running model inference: ${e.message}")
            return 0.0f
        }

        outputBuffer.rewind()
        return outputBuffer.asFloatBuffer().get(0)
    }

    fun close() {
        interpreter?.close()
    }
}