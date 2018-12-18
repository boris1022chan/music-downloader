package com.example.boris.musicdownloader.presentations

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import com.example.boris.musicdownloader.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val discoverFragment: DiscoverFragment by lazy { DiscoverFragment() }
    private var downloadFragment: DownloadFragment = createDownloadFragment()
    private val songLibraryFragment: SongLibraryFragment by lazy { SongLibraryFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.top_toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Music Downloader"
        bottom_nav_view.setOnNavigationItemSelectedListener(bottomNavViewListener)

        val fm = supportFragmentManager.beginTransaction()
        fm.apply {
            replace(R.id.main_frame, discoverFragment)
            disallowAddToBackStack()
            commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handleShareIntent(intent)
    }

    private val bottomNavViewListener: (menuItem: MenuItem) -> Boolean = { menuItem ->
        val fm = supportFragmentManager.beginTransaction()
        when (menuItem.itemId) {
            R.id.discover_tab -> {
                Log.d(TAG, "discover tab pressed")
                fm.replace(R.id.main_frame, discoverFragment)
                fm.commit()
            }
            R.id.download_tab -> {
                Log.d(TAG, "download tab pressed")
                fm.replace(R.id.main_frame, downloadFragment)
                fm.commit()
            }
            R.id.song_library_tab -> {
                Log.d(TAG, "song library tab pressed")
                fm.replace(R.id.main_frame, songLibraryFragment)
                fm.commit()
            }
        }
        true
    }

    private fun createDownloadFragment(title: String = "", link: String = ""): DownloadFragment {
        if (title.isBlank() && link.isBlank()) return DownloadFragment()
        val bundle = Bundle().apply {
            putString("YOUTUBE_TITLE", title)
            putString("YOUTUBE_LINK", link)
        }
        return DownloadFragment().apply { arguments = bundle }
    }

    private fun handleShareIntent(intent: Intent?) {
        intent?.extras?.let {
            val youtubeTitle = it.getString("YOUTUBE_TITLE") ?: ""
            val youtubeLink = it.getString("YOUTUBE_LINK") ?: ""

            if (youtubeLink.isBlank()) return
            downloadFragment = createDownloadFragment(youtubeTitle, youtubeLink)

            val fm = supportFragmentManager.beginTransaction()
            fm.apply {
                replace(R.id.main_frame, downloadFragment)
                bottom_nav_view.selectedItemId = R.id.download_tab
                disallowAddToBackStack()
                commit()
            }
        }
    }
}
