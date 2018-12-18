package com.example.boris.musicdownloader.presentations

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.boris.musicdownloader.R


class DiscoverFragment : Fragment() {

    private lateinit var discoverContentWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_discover, container, false)

        discoverContentWebView = v.findViewById(R.id.discover_webview)
        discoverContentWebView.apply {
            webViewClient = youtubeWebClient
            settings.javaScriptEnabled = true
            loadUrl("https://m.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ")
        }

        return v
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.discover_fragment_action_bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_download -> {
                downloadOptionAction()
                true
            }
        }
        return false
    }

    private val youtubeWebClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }
    }

    private fun downloadOptionAction() {
        Log.d(TAG, "download option clicked: ${discoverContentWebView.url}")
        val intent = Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("YOUTUBE_LINK", discoverContentWebView.url)
        }
        startActivity(intent)
    }

    companion object {
        private val TAG = "DiscoverFragment"
    }
}
