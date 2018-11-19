package com.example.boris.musicdownloader.data

import android.content.Context
import android.net.Uri
import android.util.Log

data class Song(
    val id: Long,
    val title: String,
    val artist: String
)

class SongRepository {

    private val TAG = "Song Repository"

    private var songList: ArrayList<Song> = ArrayList()
    private var curSongPos: Int = 0

    fun fetchSongsFromSystem(context: Context): ArrayList<Song> {
        val musicCursor = context.contentResolver.query(
            musicUri, null, null, null, null)

        Log.d(TAG, "music cursor count = ${musicCursor?.count ?: 0}")
        musicCursor?.let {
            //get columns
            val idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            //add songs to list
            val songs = mutableListOf<Song>()
            while (it.moveToNext()) {
                val thisId = it.getLong(idColumn)
                val thisTitle = it.getString(titleColumn)
                val thisArtist = it.getString(artistColumn)
                songs.add(Song(thisId, thisTitle, thisArtist))
            }
            songList = songs as ArrayList<Song>
        }

        return songList
    }

    fun getSongList(): ArrayList<Song> {
        return songList
    }

    fun setCurSongPosition(pos: Int) {
        curSongPos = pos
    }

    fun getCurSong(): Song {
        return songList.get(curSongPos)
    }

    companion object {
        private val musicUri: Uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val instance by lazy { SongRepository() }
    }
}