package io.traxa.services

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Process files.
 * Es: split videos, tag images with gps data, merge SRT to mp4....
 */
class FileProcessingService : KoinComponent {

    private val ffmpeg: FFmpeg by inject()

    /**
     * Merge subtitle file with mp4 video.
     * Es: used to merge GPS data with video
     *
     * @param srt Subtitle file
     * @param video Mp4 video file
     */
    fun mergeSrtWithVideo(srt: File, video: File, outputFile: File) {
        val result = ffmpeg.execute("-i ${video.absolutePath} -i ${srt.absolutePath} " +
                "-c copy -c:s mov_text -reset_timestamps 1 -map_metadata 0 " +
                outputFile.absolutePath
        )

        if (result != 0) throw Exception("Unable to merge srt with video file")
    }
}