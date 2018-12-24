package com.example.boris.musicdownloader.presenters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.os.Environment.DIRECTORY_MUSIC
import android.util.Log
import com.example.boris.musicdownloader.presentations.DownloadFragment
import com.example.boris.musicdownloader.usecases.YoutubeAudioDownloadUseCaseImpl
import com.example.boris.musicdownloader.utils.YoutubeUtil
import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import com.github.axet.vget.vhs.VimeoInfo
import com.github.axet.vget.vhs.YouTubeInfo
import com.github.axet.wget.SpeedInfo
import com.github.axet.wget.info.DownloadInfo
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern


interface DownloadFragmentPresenter {
    fun downloadButtonAction(input: String)
}

class DownloadFragmentPresenterImpl(val view: DownloadFragment) : DownloadFragmentPresenter {

    // TODO: fix tag
    private val TAG = "DiscoverFragmentPresent"

    private val usecase by lazy { YoutubeAudioDownloadUseCaseImpl() }

    override fun downloadButtonAction(input: String) {
        val link = input.trim()
        if (YoutubeUtil.checkValidYoutubeUri(link)) {
            val token = YoutubeUtil.extractYoutubeId(link)
            if (!token.isBlank()) {
                Log.d("Boris", "converted: ${convertLinkFormat(token)}")
                downloadMusic(convertLinkFormat(token))
            }
        }
    }

    fun convertLinkFormat(token: String): String {
        return "http://www.youtube.com/watch?v=$token"
    }

    @SuppressLint("CheckResult")
    private fun downloadMusic(link: String) {
        if (!view.haveWritePermission()) {
            view.requestPermission()
            return
        }

        usecase.execute(URL(link), view.context!!)
            .subscribeOn(Schedulers.io())
            .subscribe({}, { e ->
                e.printStackTrace()
            })
    }

    private fun externalStorageAvail(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    companion object {
        val pattern: Pattern by lazy { Pattern.compile("^http(s?)://(?:www\\.)?youtu(?:be\\.com/watch\\?v=|\\.be/)([\\w\\-_]*)(.*)") }
    }
}
