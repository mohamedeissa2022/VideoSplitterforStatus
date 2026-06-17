package com.mohadev.videosplitterforstatus.data.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

fun splitVideoWithProgress(
    context: Context,
    uri: Uri,
    outputDir: File,
    segmentSeconds: Int,
    filePrefix: String = "part", 
    onProgress: (Int) -> Unit
) {
    val extractor = MediaExtractor()

    try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            android.util.Log.e("VideoSplit", "Could not open file descriptor for URI: $uri")
            return
        }
        
        pfd.use {
            extractor.setDataSource(it.fileDescriptor)
        }

        var videoTrack = -1
        var audioTrack = -1
        val formatMap = HashMap<Int, MediaFormat>()

        val trackCount = extractor.trackCount
        if (trackCount == 0) {
            android.util.Log.e("VideoSplit", "No tracks found in video")
            return
        }

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

            if (mime.startsWith("video/")) videoTrack = i
            if (mime.startsWith("audio/")) audioTrack = i

            formatMap[i] = format
        }

        if (videoTrack == -1) {
            android.util.Log.e("VideoSplit", "No video track found")
            return
        }

        val videoFormat = formatMap[videoTrack]!!
        val durationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) videoFormat.getLong(MediaFormat.KEY_DURATION) else 0L
        val segmentUs = segmentSeconds * 1_000_000L

        extractor.selectTrack(videoTrack)
        if (audioTrack != -1) extractor.selectTrack(audioTrack)

        var segmentIndex = 0
        var currentSegmentStartUs = 0L
        var segmentFirstSampleTimeUs = -1L

        var muxer: MediaMuxer? = null
        var videoMuxTrack = -1
        var audioMuxTrack = -1

        val buffer = ByteBuffer.allocate(5 * 1024 * 1024) 
        val bufferInfo = MediaCodec.BufferInfo()

        var lastReportedProgress = -1

        while (true) {
            val sampleTrackIndex = extractor.sampleTrackIndex
            if (sampleTrackIndex == -1) break

            val sampleTime = extractor.sampleTime
            val sampleFlags = extractor.sampleFlags
            val isVideoKeyFrame = (sampleTrackIndex == videoTrack) && 
                    ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0)

            if (isVideoKeyFrame && (sampleTime >= currentSegmentStartUs + segmentUs)) {
                muxer?.let {
                    try {
                        it.stop()
                        it.release()
                    } catch (e: Exception) {
                        android.util.Log.e("VideoSplit", "Error closing muxer", e)
                    }
                }
                muxer = null
                segmentIndex++
                currentSegmentStartUs = sampleTime
                segmentFirstSampleTimeUs = -1L
            }

            if (muxer == null) {
                val outputFile = File(outputDir, "${filePrefix}_part_$segmentIndex.mp4")
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                
                if (videoTrack != -1) {
                    videoMuxTrack = muxer.addTrack(extractor.getTrackFormat(videoTrack))
                }
                if (audioTrack != -1) {
                    audioMuxTrack = muxer.addTrack(extractor.getTrackFormat(audioTrack))
                }
                muxer.start()
                segmentFirstSampleTimeUs = sampleTime
            }

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            
            val pts = sampleTime - segmentFirstSampleTimeUs
            bufferInfo.presentationTimeUs = if (pts < 0L) 0L else pts
            bufferInfo.flags = sampleFlags

            try {
                when (sampleTrackIndex) {
                    videoTrack -> muxer.writeSampleData(videoMuxTrack, buffer, bufferInfo)
                    audioTrack -> if (audioMuxTrack != -1) muxer.writeSampleData(audioMuxTrack, buffer, bufferInfo)
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoSplit", "Muxer write error", e)
            }

            if (durationUs > 0 && sampleTrackIndex == videoTrack) {
                val currentProgress = ((sampleTime.toFloat() / durationUs.toFloat()) * 100).toInt().coerceIn(0, 100)
                if (currentProgress >= lastReportedProgress + 2 || currentProgress == 100) {
                    onProgress(currentProgress)
                    lastReportedProgress = currentProgress
                }
            }

            extractor.advance()
        }

        muxer?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                android.util.Log.e("VideoSplit", "Error closing final muxer", e)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("VideoSplit", "Critical splitter crash", e)
    } finally {
        try { extractor.release() } catch (e: Exception) {}
    }
}
