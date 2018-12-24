package com.example.boris.musicdownloader.entities

import android.os.Environment
import com.github.axet.vget.VGet
import com.github.axet.vget.info.VGetParser
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import com.github.axet.vget.vhs.VimeoInfo
import com.github.axet.vget.vhs.YouTubeInfo
import com.github.axet.wget.SpeedInfo
import com.github.axet.wget.info.DownloadInfo
import io.reactivex.Completable
import io.reactivex.Single
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class VGetDownloader(
    private val url: URL
) {

    private var user: VGetParser = VGet.parser(url)
    private var videoInfo: VideoInfo
    private var v: VGet
    private var notify: VGetStatus

    init {
        videoInfo = user.info(url)
        v = VGet(videoInfo, vGetPath)
        notify = VGetStatus(videoInfo)
    }

    fun extract(url: URL): Completable {
        return Completable.fromAction {
            val stop = AtomicBoolean(false)
            v.extract(user, stop, notify)
        }
    }

    fun download(): Completable {
        return Completable.fromAction {
            val stop = AtomicBoolean(false)
            v.download(user, stop, notify)
        }
    }

    fun getTitle(): String {
        return videoInfo.title.replace("/", "")
    }

    fun getMp4Path(): String {
        return videoInfo.info?.let {
            it[0].targetFile.toString()
        } ?: ""
    }

    fun getWebmPath(): String {
        return videoInfo.info?.let {
            it[1].targetFile.toString()
        } ?: ""
    }

    companion object {
        private val vGetPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    }
}

data class ExtractedInfo(
    val user: VGetParser,
    val videoInfo: VideoInfo
)

private class VGetStatus(var videoinfo: VideoInfo) : Runnable {
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

private fun formatSpeed(s: Long): String {
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