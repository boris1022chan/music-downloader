package com.example.boris.musicdownloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.boris.musicdownloader.R
import com.example.boris.musicdownloader.data.SongRepository


class MusicPlaybackService : MediaBrowserServiceCompat(), MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    private val TAG = "MusicPlayBackService"

    private lateinit var service: MediaBrowserServiceCompat
    private lateinit var mMusicSessionCompact: MediaSessionCompat
    private var playbackState: Boolean = false
    private val player by lazy {
        MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            val attribute = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            setAudioAttributes(attribute)
            setOnPreparedListener(this@MusicPlaybackService)
        }
    }
    private val songRepository = SongRepository.instance

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mMusicSessionCompact, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        service = this

        val musicButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mMusicSessionCompact = MediaSessionCompat(applicationContext, TAG, musicButtonReceiver, null).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setClass(service, MediaButtonReceiver::class.java)
            }
            val pendingIntent = PendingIntent.getBroadcast(service, 0, mediaButtonIntent, 0)
            setMediaButtonReceiver(pendingIntent)

            setCallback(musicSessionCallback)
            setSessionToken(sessionToken)
        }

        registerReceiver(mNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            build()
        }
        am.abandonAudioFocusRequest(mFocusRequest)
        unregisterReceiver(mNoisyReceiver)
        NotificationManagerCompat.from(this).cancel(1)
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
        mMusicSessionCompact.isActive = true
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        buildForegroundNotification()
        mp?.start()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val musicSessionCallback = object: MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "onplay")

            if (!successfullyRetrievedAudioFocus()) {
                Log.e(TAG, "no audio focus, return")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(applicationContext, MusicPlaybackService::class.java))
            } else {
                startService(Intent(applicationContext, MusicPlaybackService::class.java))
            }

            player.run {
                reset()
                val song = songRepository.getCurSong()
                val trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.id)
                try {
                    setDataSource(applicationContext, trackUri)
                    mMusicSessionCompact.setMetadata(MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artist)
                        .build())
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e("Music Service", "Error setting data source")
                    e.printStackTrace()
                }
            }
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "onpause")

            player.pause()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            buildForegroundNotification()
            service.stopForeground(false)
        }

        override fun onStop() {
            super.onStop()
            Log.d(TAG, "onstop")

            service.stopSelf()
            mMusicSessionCompact.isActive = false
            player.stop()
            service.stopForeground(false)
        }
    }

    private val mNoisyReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    private fun successfullyRetrievedAudioFocus(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
            build()
        }
        val result = am.requestAudioFocus(mFocusRequest)
        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
            playbackState = true
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
            playbackState = false
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMusicSessionCompact.setPlaybackState(playbackstateBuilder.build())
    }

    private fun buildForegroundNotification() {
        val controller = mMusicSessionCompact.controller
        val context = this@MusicPlaybackService

        val builder = NotificationCompat.Builder(context, MY_MUSIC_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.notification_music)
            setContentTitle(songRepository.getCurSong().title)
            setContentText(songRepository.getCurSong().artist)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            priority = NotificationCompat.PRIORITY_LOW
            setDefaults(0)

            setContentIntent(controller.sessionActivity)

            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            if (playbackState) {
                addAction(
                    R.drawable.ic_pause, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            } else {
                addAction(
                    R.drawable.ic_play, "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                )
            }

            setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(sessionToken))
        }

        createNotificationChannel()
        startForeground(1, builder.build())
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = MY_MUSIC_CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(MY_MUSIC_CHANNEL_ID, name, importance)
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val MY_MUSIC_ROOT_ID = "media_root_id"
        private const val MY_MUSIC_CHANNEL_ID = "media_channel_id"
    }
}
