package com.example.boris.musicdownloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaMetadata
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.boris.musicdownloader.R


interface MusicNotificationManager {
    fun createMusicChannel()
    fun buildForegroundService(state: Int)

    companion object {
        const val MUSIC_FOREGROUND_SERVICE_CHANNEL_ID = "Background music"
    }
}

class MusicNotificationManagerImpl(
    private var mContext: Context,
    private var session: MediaSessionCompat
): MusicNotificationManager {

    init {
        createMusicChannel()
    }

    override fun buildForegroundService(state: Int) {
        val controller = session.controller
        val metadata = controller.metadata
        val id = MusicNotificationManager.MUSIC_FOREGROUND_SERVICE_CHANNEL_ID

        val builder = NotificationCompat.Builder(mContext, id).apply {
            setSmallIcon(R.drawable.notification_music)
            setContentTitle(metadata.bundle.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
            setContentText(metadata.bundle.getString(MediaMetadata.METADATA_KEY_ARTIST))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            priority = NotificationCompat.PRIORITY_LOW
            setDefaults(0)

            setContentIntent(controller.sessionActivity)

            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    mContext,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            if (state == PlaybackStateCompat.STATE_PLAYING) {
                addAction(
                    R.drawable.ic_pause, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mContext,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            } else {
                addAction(
                    R.drawable.ic_play, "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mContext,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                )
            }

            setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(session.sessionToken))
        }

        (mContext as MusicPlaybackService).startForeground(1, builder.build())
    }

    override fun createMusicChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = MusicNotificationManager.MUSIC_FOREGROUND_SERVICE_CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(MusicNotificationManager.MUSIC_FOREGROUND_SERVICE_CHANNEL_ID, name, importance)
            val notificationManager =
                (mContext as MusicPlaybackService).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}