package com.example.boris.musicdownloader.presentations

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.boris.musicdownloader.R
import com.example.boris.musicdownloader.presenters.DiscoverFragmentPresenterImpl


class DiscoverFragment : Fragment() {

    private val TAG = "DiscoverFragment"
    private val REQUEST_STORAGE = 123

    private val presenter by lazy { DiscoverFragmentPresenterImpl(this) }
    private var title: String = ""
    private var link: String = ""
    private lateinit var uriInput: TextView
    private lateinit var downloadBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let {
            title = it.getString("YOUTUBE_TITLE") ?: ""
            link = it.getString("YOUTUBE_LINK") ?: ""
            Log.d(TAG, "args: $title, $link")
        }
        val v = inflater.inflate(R.layout.fragment_discover, container, false)

        uriInput = v.findViewById(R.id.url_input)
        if (link.isNotBlank()) uriInput.text = link
        downloadBtn = v.findViewById(R.id.download_button) as Button
        downloadBtn.setOnClickListener {
            presenter.downloadButtonAction(uriInput.text.toString())
        }

        return v
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            REQUEST_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.downloadButtonAction(uriInput.text.toString())
                }
            }
        }
    }

    fun haveWritePermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    fun requestPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE
        )
    }
}
