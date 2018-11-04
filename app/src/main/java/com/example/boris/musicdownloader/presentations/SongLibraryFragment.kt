package com.example.boris.musicdownloader.presentations

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.boris.musicdownloader.R
import com.example.boris.musicdownloader.data.SongRepository
import com.example.boris.musicdownloader.services.MusicPlaybackService


class SongLibraryFragment : Fragment() {

    private val TAG = "SongLibraryFragment"
    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_CONTENT: Int = 0

    private lateinit var songListView: RecyclerView
    private lateinit var songListAdapter: SongListAdapter
    private val songRepository = SongRepository.instance

    private lateinit var mMusicBrowser: MediaBrowserCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_song_library, container, false)

        songListView = v.findViewById(R.id.song_list)
        songListView.apply {
            hasFixedSize()
            layoutManager = LinearLayoutManager(v.context)
            songListAdapter = SongListAdapter(this@SongLibraryFragment)
            adapter = songListAdapter
        }
        getSongList()

        mMusicBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, MusicPlaybackService::class.java),
                musicConnectionCallback,
                null
        )

        return v
    }

    override fun onStart() {
        super.onStart()
        mMusicBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(activity!!).unregisterCallback(controllerCallback)
        mMusicBrowser.disconnect()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_CONTENT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSongList()
                }
            }
        }
    }

    private val musicConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mMusicBrowser.sessionToken.also {
                val musicController = MediaControllerCompat(context, it)
                MediaControllerCompat.setMediaController(activity!!, musicController)
                musicController.registerCallback(controllerCallback)
            }

        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "metadata changed")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "playstate changed")
        }
    }

    private fun getSongList() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_CONTENT)
        } else {
            val songList = songRepository.fetchSongsFromSystem(this.context!!)
            songListAdapter.updateSongList(songList)
        }
    }

    fun songPicked(pos: Int) {
        songRepository.setCurSongPosition(pos)
        val controller = MediaControllerCompat.getMediaController(activity!!)
        controller.transportControls.play()
    }

}
