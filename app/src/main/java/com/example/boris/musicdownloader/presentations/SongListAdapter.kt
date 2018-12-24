package com.example.boris.musicdownloader.presentations

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.boris.musicdownloader.R
import com.example.boris.musicdownloader.entities.Song

class SongListAdapter(
    private val parent: SongLibraryFragment
): RecyclerView.Adapter<SongListAdapter.SongListViewHolder>() {

    private var songs: List<Song> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongListViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_song_list_item, parent, false)
        return SongListViewHolder(v)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(viewHolder: SongListViewHolder, pos: Int) {
        viewHolder.songTitle.text = songs[pos].title
        viewHolder.songArtist.text = songs[pos].artist
        viewHolder.itemView.setOnClickListener{
            parent.songPicked(pos)
        }
    }

    fun updateSongList(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }


    class SongListViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val songTitle = v.findViewById(R.id.song_title) as TextView
        val songArtist = v.findViewById(R.id.song_artist) as TextView
    }
}