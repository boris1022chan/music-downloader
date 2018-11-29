package com.example.boris.musicdownloader.presenters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.os.Environment.DIRECTORY_MUSIC
import android.util.Log
import com.example.boris.musicdownloader.presentations.DownloadFragment
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

    override fun downloadButtonAction(input: String) {
        val link = input.trim()
        if (checkValidYoutubeUri(link)) {
            val token = extractYoutubeId(link)
            if (!token.isBlank()) {
                Log.d("Boris", "converted: ${convertLinkFormat(token)}")
                downloadMusic(convertLinkFormat(token))
            }
        }
    }

    fun checkValidYoutubeUri(uri: String): Boolean {
        val matcher = pattern.matcher(uri)
        Log.d(TAG, "link validity: ${matcher.matches()}")
        return matcher.matches()
    }

    fun extractYoutubeId(url: String): String {
        val matcher = pattern.matcher(url)

        if (matcher.find()) {
            return matcher.group(2)
        }
        return ""
    }

    fun convertLinkFormat(token: String): String {
        return "http://www.youtube.com/watch?v=$token"
    }

    private fun downloadMusic(link: String) {
        if (!view.haveWritePermission()) {
            view.requestPermission()
            return
        }

        DownloadMusicTask(view.context!!).execute(URL(link))
    }

    private fun externalStorageAvail(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    companion object {
        val pattern: Pattern by lazy { Pattern.compile("^http(s?)://(?:www\\.)?youtu(?:be\\.com/watch\\?v=|\\.be/)([\\w\\-_]*)(.*)") }
    }
}


private class DownloadMusicTask(
    private val mContext: Context
) : AsyncTask<URL, Unit, Unit>() {

    override fun doInBackground(vararg params: URL?) {
        if (params[0] == null) {
            Log.d("Boris", "link is not received")
            return
        }

        Log.d("Boris", "start download")
        val linkUrl = params[0]
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        try {
            val user = VGet.parser(linkUrl)
            val videoInfo = user.info(linkUrl)
            val v = VGet(videoInfo, path)
            val notify = VGetStatus(videoInfo)
            val stop: AtomicBoolean = AtomicBoolean(false)

            v.extract(user, stop, notify)
            val title = videoInfo.title.replace("/", "")

            System.out.println("Title: $title")
            val list = videoInfo.info
            if (list != null) {

                for (d in list) {
                    println("Download URL: " + d.source)
                }
            }

            v.download(user, stop, notify)

            val mp4File = File(videoInfo.info.get(0).targetFile.toString())
            if (mp4File.exists()) {
                Log.d("Boris", "Boris downloade complete delete file")
                mp4File.delete()
            } else {
                Log.d("Boris", "Boris downloade complete not found ")
            }

            val ffmpeg = FFmpeg.getInstance(mContext)
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
            try {
                ffmpeg.execute(
                    arrayOf(
                        "-i",
                        videoInfo.info.get(1).targetFile.toString(),
                        "${Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)}/$title.mp3"
                    ), object : ExecuteBinaryResponseHandler() {

                        override fun onStart() {}

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

                            val webmFile = File(videoInfo.info.get(1).targetFile.toString())
                            if (webmFile.exists()) {
                                Log.d("Boris", "Boris convert complete delete file")
                                webmFile.delete()
                            } else {
                                Log.d("Boris", "Boris convert complete not found ")
                            }

                            val mp3File = File("${Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)}/$title.mp3")
                            mContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mp3File)))
                        }
                    })
            } catch (e: FFmpegCommandAlreadyRunningException) {
                // Handle if FFmpeg is already running
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

internal class VGetStatus(var videoinfo: VideoInfo) : Runnable {
    var last: Long = 0

    var map: MutableMap<VideoFileInfo, SpeedInfo> = HashMap()

    fun getSpeedInfo(dinfo: VideoFileInfo): SpeedInfo {
        var speedInfo = map[dinfo]
        if (speedInfo == null) {
            speedInfo = SpeedInfo()
            speedInfo!!.start(dinfo.count)
            map[dinfo] = speedInfo!!
        }
        return speedInfo
    }

    override fun run() {
        val dinfoList = videoinfo.info

        // notify app or save download state
        // you can extract information from DownloadInfo info;
        when (videoinfo.state) {
            VideoInfo.States.EXTRACTING, VideoInfo.States.EXTRACTING_DONE, VideoInfo.States.DONE -> {
                if (videoinfo is YouTubeInfo) {
                    val i = videoinfo as YouTubeInfo
                    println((videoinfo.getState()).toString() + " " + i.videoQuality)
                } else if (videoinfo is VimeoInfo) {
                    val i = videoinfo as VimeoInfo
                    println((videoinfo.getState()).toString() + " " + i.videoQuality)
                } else {
                    println("downloading unknown quality")
                }
                for (d in videoinfo.info) {
                    val speedInfo = getSpeedInfo(d)
                    speedInfo.end(d.count)
                    println(
                        String.format(
                            "file:%d - %s (%s)", dinfoList!!.indexOf(d), d.targetFile,
                            formatSpeed(speedInfo.averageSpeed.toLong())
                        )
                    )
                }
            }
            VideoInfo.States.ERROR -> {
                println((videoinfo.getState()).toString() + " " + videoinfo.delay)

                if (dinfoList != null) {
                    for (dinfo in dinfoList!!) {
                        println(
                            "file:" + dinfoList!!.indexOf(dinfo) + " - " + dinfo.exception + " delay:"
                                    + dinfo.delay
                        )
                    }
                }
            }
            VideoInfo.States.RETRYING -> {
                println((videoinfo.getState()).toString() + " " + videoinfo.delay)

                if (dinfoList != null) {
                    for (dinfo in dinfoList!!) {
                        println(
                            ("file:" + dinfoList!!.indexOf(dinfo) + " - " + dinfo.state + " "
                                    + dinfo.exception + " delay:" + dinfo.delay)
                        )
                    }
                }
            }
            VideoInfo.States.DOWNLOADING -> {
                val now = System.currentTimeMillis()
                if (now - 1000 > last) {
                    last = now

                    var parts = ""

                    for (dinfo in dinfoList!!) {
                        val speedInfo = getSpeedInfo(dinfo)
                        speedInfo.step(dinfo.count)

                        val pp = dinfo.parts
                        if (pp != null) {
                            // multipart download
                            for (p in pp!!) {
                                if (p.state.equals(DownloadInfo.Part.States.DOWNLOADING)) {
                                    parts += String.format(
                                        "part#%d(%.2f) ", p.number,
                                        p.count / p.length.toFloat()
                                    )
                                }
                            }
                        }
                        println(
                            String.format(
                                "file:%d - %s %.2f %s (%s)", dinfoList!!.indexOf(dinfo),
                                videoinfo.state, dinfo.count / dinfo.length.toFloat(), parts,
                                formatSpeed(speedInfo.currentSpeed.toLong())
                            )
                        )
                    }
                }
            }
            else -> {
            }
        }
    }
}

fun formatSpeed(s: Long): String {
    if (s > 0.1 * 1024.0 * 1024.0 * 1024.0) {
        val f = s.toFloat() / 1024f / 1024f / 1024f
        return String.format("%.1f GB/s", f)
    } else if (s > 0.1 * 1024.0 * 1024.0) {
        val f = s.toFloat() / 1024f / 1024f
        return String.format("%.1f MB/s", f)
    } else {
        val f = s / 1024f
        return String.format("%.1f kb/s", f)
    }
}