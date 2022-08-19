package io.traxa.services

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class FFmpeg {

    private inner class FFmpegException(message: String) : Exception(message)


    fun execute(command: String): Int {
        val session = FFmpegKit.execute("$command -y")
        if (!ReturnCode.isSuccess(session.returnCode)) {
            //Log error
        }

        return session.returnCode.value
    }
}