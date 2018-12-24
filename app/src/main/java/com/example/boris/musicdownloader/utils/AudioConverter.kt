package com.example.boris.musicdownloader.utils

import android.content.Context
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException

class AudioConverter() {

    private lateinit var ffmpeg: FFmpeg

    private fun initFfmpeg(context: Context) {
        ffmpeg = FFmpeg.getInstance(context)
        try {
            ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {

                override fun onStart() {}

                override fun onFailure() {}

                override fun onSuccess() {}

                override fun onFinish() {}
            })
        } catch (e: FFmpegNotSupportedException) {
            // Handle if FFmpeg is not supported by device
        }
    }

    fun convertWebmToMp3(context: Context, targetPath: String, destPath: String, onFinishFunc: ()->Unit) {
        if (!::ffmpeg.isInitialized) initFfmpeg(context)
        ffmpeg.execute(
            arrayOf("-i", targetPath, destPath),
            object : ExecuteBinaryResponseHandler() {
                override fun onProgress(message: String?) {
                    Log.d("ffmpegevent", "onProgress: " + message!!)
                }

                override fun onFailure(message: String?) {
                    Log.d("ffmpegevent", "onProgress: " + message!!)
                    Log.d("ffmpegevent", "onFailure: ")
                }

                override fun onSuccess(message: String?) {
                    Log.d("ffmpegevent", "onSuccess: ")

                }

                override fun onFinish() {
                    Log.d("ffmpegevent", "onProgress: Finished")
                    onFinishFunc()
                }
            })
    }
}