package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "SoundEngine"
private const val SAMPLE_RATE = 44100

class SoundEngine {
    private val clickExecutor = Executors.newSingleThreadExecutor()
    private val isMusicEnabled = AtomicBoolean(true)
    private val isForeground = AtomicBoolean(true)
    private val isReleased = AtomicBoolean(false)

    @Volatile
    private var musicThread: Thread? = null

    private val clickPcm: ShortArray by lazy { generateClickPcm() }

    private var clickTrack: AudioTrack? = null

    init {
        initClickTrack()
    }

    private fun initClickTrack() {
        try {
            val bufferBytes = clickPcm.size * 2
            clickTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build().apply {
                    write(clickPcm, 0, clickPcm.size)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize click track", e)
        }
    }

    private fun generateClickPcm(): ShortArray {
        val durationSec = 0.022
        val count = (SAMPLE_RATE * durationSec).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 210.0)
            // Warm tactile mechanical/wooden click: crisp transient + warm body
            val tone = 0.65 * sin(2.0 * PI * 980.0 * t) +
                0.35 * sin(2.0 * PI * 360.0 * t) +
                0.20 * cos(2.0 * PI * 1850.0 * t) * exp(-t * 420.0)
            val sample = (tone * env * 0.55 * Short.MAX_VALUE).toInt()
            sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun playClick() {
        if (isReleased.get()) return
        clickExecutor.execute {
            try {
                val track = clickTrack ?: return@execute
                if (track.state != AudioTrack.STATE_INITIALIZED) return@execute
                track.pause()
                track.flush()
                track.setPlaybackHeadPosition(0)
                track.play()
            } catch (e: Exception) {
                Log.w(TAG, "Click playback skipped", e)
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled.set(enabled)
        if (enabled && isForeground.get()) {
            startMusicIfNeeded()
        } else {
            stopMusicThread()
        }
    }

    fun onForeground() {
        isForeground.set(true)
        if (isMusicEnabled.get()) {
            startMusicIfNeeded()
        }
    }

    fun onBackground() {
        isForeground.set(false)
        stopMusicThread()
    }

    @Synchronized
    private fun startMusicIfNeeded() {
        if (isReleased.get() || !isMusicEnabled.get() || !isForeground.get()) return
        if (musicThread?.isAlive == true) return

        val thread = Thread {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(minBuf * 2)
                    .build()

                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    return@Thread
                }

                track.play()

                // 16-second soothing study progression: Cmaj9 -> Am9 -> Fmaj7 -> G6
                val chords = arrayOf(
                    doubleArrayOf(261.63, 329.63, 392.00, 493.88, 587.33), // Cmaj9
                    doubleArrayOf(220.00, 261.63, 329.63, 392.00, 493.88), // Am9
                    doubleArrayOf(174.61, 220.00, 261.63, 329.63, 392.00), // Fmaj7/9
                    doubleArrayOf(196.00, 246.94, 293.66, 329.63, 440.00)  // G6add9
                )
                val melodyNotes = arrayOf(
                    doubleArrayOf(523.25, 659.25, 783.99, 659.25),
                    doubleArrayOf(440.00, 523.25, 659.25, 587.33),
                    doubleArrayOf(349.23, 440.00, 523.25, 659.25),
                    doubleArrayOf(392.00, 493.88, 587.33, 523.25)
                )

                val chunkSize = 2048
                val buffer = ShortArray(chunkSize)
                var sampleIndex = 0L
                val loopSamples = SAMPLE_RATE * 16L
                val chordSamples = SAMPLE_RATE * 4L

                while (!Thread.currentThread().isInterrupted &&
                    !isReleased.get() &&
                    isMusicEnabled.get() &&
                    isForeground.get()
                ) {
                    for (i in 0 until chunkSize) {
                        val posInLoop = (sampleIndex % loopSamples)
                        val chordIdx = ((posInLoop / chordSamples).toInt()).coerceIn(0, 3)
                        val posInChord = (posInLoop % chordSamples).toDouble() / SAMPLE_RATE
                        val t = sampleIndex.toDouble() / SAMPLE_RATE

                        // Smooth 0.35s fade-in/out envelope per chord to avoid any clicks
                        val chordEnv = when {
                            posInChord < 0.35 -> posInChord / 0.35
                            posInChord > 3.65 -> (4.0 - posInChord) / 0.35
                            else -> 1.0
                        }.coerceIn(0.0, 1.0)

                        val chord = chords[chordIdx]
                        var padSum = 0.0
                        for (freq in chord) {
                            padSum += sin(2.0 * PI * freq * t) * 0.14 +
                                sin(2.0 * PI * (freq * 2.0) * t) * 0.025
                        }

                        // Gentle breathing LFO
                        val lfo = 0.85 + 0.15 * sin(2.0 * PI * 0.22 * t)
                        val padSignal = padSum * chordEnv * lfo

                        // Soft celesta/kalimba arpeggio note every 1.0 second
                        val noteBeat = (posInChord / 1.0).toInt().coerceIn(0, 3)
                        val noteLocalT = posInChord - noteBeat * 1.0
                        val noteFreq = melodyNotes[chordIdx][noteBeat]
                        val noteEnv = exp(-noteLocalT * 2.6) * (1.0 - exp(-noteLocalT * 45.0))
                        val bellSignal = (sin(2.0 * PI * noteFreq * t) * 0.16 +
                            sin(2.0 * PI * noteFreq * 2.0 * t) * 0.04) * noteEnv

                        val mixed = (padSignal + bellSignal) * 0.42
                        val pcm = (mixed * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                        buffer[i] = pcm
                        sampleIndex++
                    }
                    track.write(buffer, 0, chunkSize)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Study music loop ended", e)
            } finally {
                try {
                    track?.pause()
                    track?.flush()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }.apply {
            name = "NabraawyStudyMusic"
            isDaemon = true
            start()
        }
        musicThread = thread
    }

    @Synchronized
    private fun stopMusicThread() {
        musicThread?.interrupt()
        musicThread = null
    }

    fun release() {
        isReleased.set(true)
        stopMusicThread()
        clickExecutor.shutdownNow()
        try {
            clickTrack?.release()
        } catch (_: Exception) {
        }
        clickTrack = null
    }
}
