package com.example.boris.musicdownloader.usecases

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.example.boris.musicdownloader.entities.VGetDownloader
import com.example.boris.musicdownloader.utils.AudioConverter
import io.reactivex.Completable
import java.io.File
import java.net.URL

interface YoutubeAudioDownloadUseCase {
    fun execute(url: URL, context: Context): Completable
}

class YoutubeAudioDownloadUseCaseImpl : YoutubeAudioDownloadUseCase {
    val ac = AudioConverter()

    override fun execute(url: URL, context: Context): Completable {
        val v = VGetDownloader(url)
        return v.extract(url)
            .concatWith(v.download())
            .concatWith(Completable.fromCallable {
                val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)}/${v.getTitle()}.mp3"
                val onAudioConverFinish = {
                    callSystemMediaScan(context, path)
                    removeFileFromPath(v.getMp4Path())
                    removeFileFromPath(v.getWebmPath())
                }
                ac.convertWebmToMp3(
                    context, v.getWebmPath(), path,
                    onAudioConverFinish
                )
            })
    }

    private fun removeFileFromPath(path: String) {
        if (path.isBlank()) return
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun callSystemMediaScan(context: Context, path: String) {
        val mp3File = File(path)
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mp3File)))
    }
}