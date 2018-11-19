package com.example.boris.musicdownloader.presentations

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.boris.musicdownloader.R
import com.example.boris.musicdownloader.presenters.DiscoverFragmentPresenter
import com.example.boris.musicdownloader.presenters.DiscoverFragmentPresenterImpl
import java.io.File
import java.util.regex.Pattern


class DiscoverFragment : Fragment() {

    private val TAG = "DiscoverFragment"
    private val REQUEST_STORAGE = 123

    private val presenter by lazy { DiscoverFragmentPresenterImpl(this) }
    private lateinit var uriInput: TextView
    private lateinit var downloadBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_discover, container, false)

        uriInput = v.findViewById(R.id.url_input)
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
