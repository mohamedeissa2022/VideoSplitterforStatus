package com.mohadev.videosplitterforstatus.domain

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
    onProgress: (Int) -> Unit
) {
    val extractor = MediaExtractor()

    context.contentResolver.openFileDescriptor(uri, "r")?.use {
        extractor.setDataSource(it.fileDescriptor)
    }

    var videoTrack = -1
    var audioTrack = -1
    val formatMap = HashMap<Int, MediaFormat>()

    val trackCount = extractor.trackCount
    for (i in 0 until trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

        if (mime.startsWith("video/")) videoTrack = i
        if (mime.startsWith("audio/")) audioTrack = i

        formatMap[i] = format
    }

    if (videoTrack == -1) return

    val videoFormat = formatMap[videoTrack]!!
    val durationUs = videoFormat.getLong(MediaFormat.KEY_DURATION)
    val segmentUs = segmentSeconds * 1_000_000L

    // Select both video and audio tracks
    if (videoTrack != -1) extractor.selectTrack(videoTrack)
    if (audioTrack != -1) extractor.selectTrack(audioTrack)

    var segmentIndex = 0
    var currentSegmentStartUs = 0L
    var segmentFirstSampleTimeUs = -1L

    var muxer: MediaMuxer? = null
    var videoMuxTrack = -1
    var audioMuxTrack = -1

    val buffer = ByteBuffer.allocate(5 * 1024 * 1024) // 5MB buffer
    val bufferInfo = MediaCodec.BufferInfo()

    try {
        while (true) {
            val sampleTrackIndex = extractor.sampleTrackIndex
            if (sampleTrackIndex == -1) {
                break
            }

            val sampleTime = extractor.sampleTime
            val sampleFlags = extractor.sampleFlags
            val isVideoKeyFrame = (sampleTrackIndex == videoTrack) && 
                    ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0)

            // Check if we reached the segment duration AND we are at a video keyframe
            if (isVideoKeyFrame && (sampleTime >= currentSegmentStartUs + segmentUs)) {
                // Close current muxer
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

            // Create new muxer if needed
            if (muxer == null) {
                val outputFile = File(outputDir, "part_$segmentIndex.mp4")
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

            // Read the sample data
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            
            // Adjust the presentation timestamp to be relative to the segment start and non-negative
            val pts = sampleTime - segmentFirstSampleTimeUs
            bufferInfo.presentationTimeUs = if (pts < 0L) 0L else pts
            bufferInfo.flags = sampleFlags

            // Write the sample to the current muxer
            when (sampleTrackIndex) {
                videoTrack -> muxer.writeSampleData(videoMuxTrack, buffer, bufferInfo)
                audioTrack -> muxer.writeSampleData(audioMuxTrack, buffer, bufferInfo)
            }

            // Report progress
            if (durationUs > 0 && sampleTrackIndex == videoTrack) {
                val progress = ((sampleTime.toFloat() / durationUs.toFloat()) * 100).toInt()
                onProgress(progress.coerceIn(0, 100))
            }

            extractor.advance()
        }
    } finally {
        muxer?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                android.util.Log.e("VideoSplit", "Error closing final muxer", e)
            }
        }
        extractor.release()
    }

    onProgress(100)
}