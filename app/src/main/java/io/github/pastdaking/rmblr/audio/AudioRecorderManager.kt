package io.github.pastdaking.rmblr.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorderManager(private val context: Context) {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _liveStreamingText = MutableStateFlow("")
    val liveStreamingText: StateFlow<String> = _liveStreamingText.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val audioBuffer = ByteArrayOutputStream()

    /**
     * @param onPcmChunk called on the recording thread with every buffer of microphone
     *        audio as it is read, so a streaming engine can put it on the wire while the
     *        user is still speaking rather than waiting for the whole clip. The array is
     *        reused between reads, so copy anything you intend to keep. The full
     *        recording is still buffered either way, because the batch engines need it
     *        and because it is the fallback if a stream dies mid-sentence.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(onPcmChunk: ((ByteArray, Int) -> Unit)? = null) {
        if (_isRecording.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed.")
                return
            }

            audioBuffer.reset()
            _audioAmplitude.value = 0f
            _liveStreamingText.value = ""
            _isRecording.value = true
            audioRecord?.startRecording()

            // Coroutine to read PCM bytes & calculate live amplitude
            recordJob = scope.launch {
                val tempBuffer = ByteArray(bufferSize)
                while (isActive && _isRecording.value) {
                    val bytesRead = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: -1
                    if (bytesRead > 0) {
                        synchronized(audioBuffer) {
                            audioBuffer.write(tempBuffer, 0, bytesRead)
                        }

                        // Straight onto the wire, before anything else is done with it.
                        // A streaming engine's whole advantage is that this happens now
                        // and not after the user lets go.
                        onPcmChunk?.invoke(tempBuffer, bytesRead)

                        // Calculate RMS amplitude for real-time waveform
                        var sum = 0.0
                        val shortCount = bytesRead / 2
                        val shortBuffer = ByteBuffer.wrap(tempBuffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        for (i in 0 until shortCount) {
                            val sample = shortBuffer.get(i).toDouble()
                            sum += sample * sample
                        }
                        val rms = Math.sqrt(sum / shortCount.coerceAtLeast(1))
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        // Exponential smoothing for aesthetic visualizer
                        _audioAmplitude.value = (_audioAmplitude.value * 0.3f) + (normalized * 2.5f).coerceIn(0f, 1f) * 0.7f
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            _isRecording.value = false
        }
    }

    /**
     * Android's SpeechRecognizer plays a "listening" earcon that cannot be muted, which
     * is the beep the operator asked us to get rid of. Transcription goes through Gemini
     * on the recorded WAV anyway, so nothing calls this now. Kept for reference only.
     */
    @Suppress("unused")
    private fun startSpeechRecognizerFallback(onLiveSpeechText: ((String) -> Unit)?) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            if (rmsdB > 0) {
                                val norm = (rmsdB / 10f).coerceIn(0f, 1f)
                                _audioAmplitude.value = (_audioAmplitude.value * 0.4f) + (norm * 0.6f)
                            }
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {}
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _liveStreamingText.value = text
                                onLiveSpeechText?.invoke(text)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _liveStreamingText.value = text
                                onLiveSpeechText?.invoke(text)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.w("AudioRecorder", "SpeechRecognizer fallback notice: ${e.message}")
            }
        }
    }

    fun stopRecording(): ByteArray {
        _isRecording.value = false
        recordJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }

        val rawPcm = synchronized(audioBuffer) { audioBuffer.toByteArray() }
        _audioAmplitude.value = 0f
        return createWavFile(rawPcm, sampleRate, 1, 16)
    }

    fun getLiveTranscript(): String = _liveStreamingText.value

    fun cancelRecording() {
        _isRecording.value = false
        recordJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // Ignore
        }
        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
        audioBuffer.reset()
        _audioAmplitude.value = 0f
        _liveStreamingText.value = ""
    }

    private fun createWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavOutput = ByteArrayOutputStream()
        wavOutput.write(header)
        wavOutput.write(pcmData)
        return wavOutput.toByteArray()
    }
}
