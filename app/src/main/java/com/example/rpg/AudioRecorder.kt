package com.example.rpg

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt

class AudioRecorder(private val context: Context) {

    private val sampleRate = 16000 // 16kHz is ideal for Gemini audio models
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat), 4096)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val tempPcmFileName = "temp_recording.pcm"
    private val outWavFileName = "voice_command.wav"

    // Callback on silence detected
    private var onSilenceCallback: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun startRecording(onSilenceDetected: () -> Unit = {}): Boolean {
        if (isRecording) return false
        onSilenceCallback = onSilenceDetected

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed!")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            val pcmFile = File(context.cacheDir, tempPcmFileName)
            recordingThread = Thread({
                writeAudioDataWithSilenceDetection(pcmFile)
            }, "AudioRecordingThread")
            recordingThread?.start()

            Log.d("AudioRecorder", "Recording started successfully with Silence Auto-Detect.")
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting voice recording: ${e.message}", e)
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join()
            recordingThread = null
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping AudioRecord: ${e.message}", e)
        }

        val pcmFile = File(context.cacheDir, tempPcmFileName)
        val wavFile = File(context.cacheDir, outWavFileName)

        return try {
            rawPcmToWavFile(pcmFile, wavFile)
            Log.d("AudioRecorder", "Converted PCM to WAV successfully. File exists: ${wavFile.exists()} (${wavFile.length()} bytes)")
            wavFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error converting PCM to WAV: ${e.message}", e)
            null
        }
    }

    private fun writeAudioDataWithSilenceDetection(file: File) {
        val data = ByteArray(bufferSize)
        var os: FileOutputStream? = null
        
        // Safe check for bufferSize to avoid any negative array or illegal size exceptions
        if (bufferSize <= 0) {
            Log.e("AudioRecorder", "Invalid buffer size: $bufferSize")
            isRecording = false
            return
        }
        
        // Voice Activity Detection parameters
        var voiceDetected = false
        var consecutiveSilenceFrames = 0
        var totalRecordedFrames = 0
        
        // Dynamically Calibrated Noise Floor tracking
        var ambientNoiseSum = 0.0
        val calibrationFramesCount = 6
        var ambientNoiseRms = 60.0 // solid default baseline
        var voiceRmsThreshold = 140.0
        var silenceRmsThreshold = 80.0
        
        val maxSilenceFramesThreshold = 25 
        
        // Hard safety limits for recording time (Max 8.5 seconds)
        var totalRecordedBytes = 0L
        val maxRecordingBytes = sampleRate * 2L * 8.5 // 8.5 seconds of PCM (16kHz 16bit = 32KB/sec)

        try {
            os = FileOutputStream(file)
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                
                // CRITICAL SAFETY FIX: Avoid infinite spinning when recorder encounters read errors or permissions revoked
                if (read <= 0) {
                    Log.e("AudioRecorder", "AudioRecord read returned error code or EOF: $read")
                    isRecording = false
                    break
                }

                // Apply a software digital gain amplification (4.0x boost) to capture low-level vocal input
                val gainFactor = 4.0f
                for (i in 0 until read step 2) {
                    if (i + 1 < read) {
                        var sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort().toFloat()
                        sample *= gainFactor
                        val clamped = sample.coerceIn(-32768f, 32767f).toInt().toShort()
                        data[i] = (clamped.toInt() and 0xFF).toByte()
                        data[i + 1] = ((clamped.toInt() shr 8) and 0xFF).toByte()
                    }
                }

                os.write(data, 0, read)
                totalRecordedBytes += read
                totalRecordedFrames++

                // Hard stop safety limit: exit if recording reaches max allowed duration
                if (totalRecordedBytes >= maxRecordingBytes) {
                    Log.i("AudioRecorder", "Safety Limit Reached: ~8.5 seconds recorded. Automatically stopping voice input.")
                    mainHandler.post {
                        onSilenceCallback?.invoke()
                    }
                    break
                }

                // Calculate Root Mean Square (RMS) to determine current audio power level
                var sumOfSquares = 0.0
                var count = 0
                for (i in 0 until read step 2) {
                    if (i + 1 < read) {
                        val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort()
                        sumOfSquares += sample * sample
                        count++
                    }
                }
                val rms = if (count > 0) sqrt(sumOfSquares / count) else 0.0

                // Dynamic Calibration Phase (first 6 frames, approx 300-400ms)
                if (totalRecordedFrames <= calibrationFramesCount) {
                    ambientNoiseSum += rms
                    if (totalRecordedFrames == calibrationFramesCount) {
                        val avgAmbient = ambientNoiseSum / calibrationFramesCount
                        ambientNoiseRms = avgAmbient.coerceIn(40.0, 220.0)
                        
                        // Relative adaptive thresholds
                        voiceRmsThreshold = maxOf(ambientNoiseRms * 1.8, 110.0)
                        silenceRmsThreshold = maxOf(ambientNoiseRms * 1.15, 70.0)
                        
                        Log.i("AudioRecorder", "VAD Calibrated - Ambient: ${ambientNoiseRms.toInt()}, Voice Thresh: ${voiceRmsThreshold.toInt()}, Silence Thresh: ${silenceRmsThreshold.toInt()}")
                    }
                    continue // Skip detection during calibration
                }

                // Print log periodically so we don't spam, but can debug active voice power
                if (totalRecordedFrames % 10 == 0) {
                    Log.d("AudioRecorder", "VAD Status - RMS: ${rms.toInt()}, voiceDetected: $voiceDetected, silenceFrames: $consecutiveSilenceFrames/$maxSilenceFramesThreshold (Thresh: V=${voiceRmsThreshold.toInt()}, S=${silenceRmsThreshold.toInt()})")
                }

                if (!voiceDetected) {
                    if (rms > voiceRmsThreshold) {
                        voiceDetected = true
                        Log.d("AudioRecorder", "VAD Detection: Active Speech Started (RMS: ${rms.toInt()} > ${voiceRmsThreshold.toInt()})")
                    }
                } else {
                    if (rms < silenceRmsThreshold) {
                        consecutiveSilenceFrames++
                        if (consecutiveSilenceFrames >= maxSilenceFramesThreshold) {
                            Log.i("AudioRecorder", "VAD Detection: Silence sustained for ~1.5s. Auto-Stopping Voice Input!")
                            mainHandler.post {
                                onSilenceCallback?.invoke()
                            }
                            break
                        }
                    } else {
                        // Reset silence counter if user continues speaking a word
                        consecutiveSilenceFrames = 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error writing PCM data and running VAD: ${e.message}", e)
        } finally {
            try {
                os?.close()
            } catch (e: IOException) {
                // Ignore
            }
        }
    }

    private fun rawPcmToWavFile(pcmFile: File, wavFile: File) {
        val longSampleRate = sampleRate.toLong()
        val channels = 1
        val byteRate = (16 * sampleRate * channels / 8).toLong()
        val totalAudioLen = pcmFile.length()
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // fmt
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Header length (bits/sample config size)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Format is PCM
        header[21] = 0
        header[22] = channels.toByte() // Channels count -> Mono
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte() // Samplerate
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte() // Byterate
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte() // Block align
        header[33] = 0
        header[34] = 16 // Bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        var out: FileOutputStream? = null
        var qIn: FileInputStream? = null
        try {
            out = FileOutputStream(wavFile)
            qIn = FileInputStream(pcmFile)
            out.write(header, 0, 44)
            val buffer = ByteArray(bufferSize)
            var bytesRead = qIn.read(buffer)
            while (bytesRead != -1) {
                out.write(buffer, 0, bytesRead)
                bytesRead = qIn.read(buffer)
            }
        } finally {
            qIn?.close()
            out?.close()
        }
    }
}
