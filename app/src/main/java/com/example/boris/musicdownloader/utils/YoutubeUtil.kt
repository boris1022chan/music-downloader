package com.example.boris.musicdownloader.utils

import android.util.Log
import com.example.boris.musicdownloader.presenters.DownloadFragmentPresenterImpl
import java.util.regex.Pattern

class YoutubeUtil {
    companion object {
        private const val TAG = "YoutubeUtil"
        private val pattern: Pattern = Pattern.compile("^http(s?)://(?:www\\.)?youtu(?:be\\.com/watch\\?v=|\\.be/)([\\w\\-_]*)(.*)")

        fun checkValidYoutubeUri(uri: String): Boolean {
            val matcher = DownloadFragmentPresenterImpl.pattern.matcher(uri)
            Log.d(TAG, "link validity: ${matcher.matches()}")
            return matcher.matches()
        }

        fun extractYoutubeId(url: String): String {
            val matcher = DownloadFragmentPresenterImpl.pattern.matcher(url)

            if (matcher.find()) {
                return matcher.group(2)
            }
            return ""
        }

        fun convertMobileToStandard(uri: String): String {
            return uri.replace("m.youtube", "youtube", ignoreCase = true)
        }
    }
}