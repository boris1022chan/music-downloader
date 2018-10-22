package com.example.boris.musicdownloader.services

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.boris.musicdownloader.data.SongRepository

class MusicPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener {

    private val TAG = "MusicPlayBackService"

    private lateinit var service: MediaBrowserServiceCompat
    private lateinit var mMusicSessionCompact: MediaSessionCompat
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private val player by lazy {
        MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            val attribute = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            setAudioAttributes(attribute)
        }
    }
    private val songRepository = SongRepository.instance

    override fun onCreate() {
        super.onCreate()

        service = this

        mMusicSessionCompact = MediaSessionCompat(this.applicationContext, TAG).apply {
            mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            setPlaybackState(mStateBuilder.build())
            setCallback(musicSessionCallback)
            setSessionToken(sessionToken)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHint: Bundle?): BrowserRoot? {
        return BrowserRoot(MY_MUSIC_ROOT_ID, null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()

        if (parentMediaId == MY_MUSIC_ROOT_ID) {
        }
        result.sendResult(mediaItems)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(TAG, "Boris mp start")
        mp?.start()
    }

    private val musicSessionCallback = object: MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {

        }

        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "onplay")

            startService(Intent(applicationContext, MusicPlaybackService::class.java))
            player.run {
                reset()
                val song = songRepository.getCurSong()
                val trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.id)
                try {
                    setDataSource(applicationContext, trackUri)
                    prepare()
                    start()
                } catch (e: Exception) {
                    Log.e("Music Service", "Error setting data source")
                    e.printStackTrace()
                }
            }
        }

        override fun onPause() {
            super.onPause()

            player.pause()
            service.stopForeground(false)
        }

        override fun onStop() {
            super.onStop()

            service.stopSelf()
            mMusicSessionCompact.isActive = false
            player.stop()
            service.stopForeground(false)
        }
    }

    companion object {
        private const val MY_MUSIC_ROOT_ID = "media_root_id"
    }
}
