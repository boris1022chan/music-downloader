package com.example.boris.musicdownloader.presentations

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.example.boris.musicdownloader.R


class DiscoverFragment : Fragment() {

    private val TAG = "DiscoverFragment"

    private lateinit var discoverContentWebView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_discover, container, false)

        discoverContentWebView = v.findViewById(R.id.discover_webview)
        discoverContentWebView.apply {
            settings.javaScriptEnabled = true
            loadUrl("https://m.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ")
        }

        return v
    }
}
