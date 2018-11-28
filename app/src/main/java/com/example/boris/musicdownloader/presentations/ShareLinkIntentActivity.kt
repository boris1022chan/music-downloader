package com.example.boris.musicdownloader.presentations

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class ShareLinkIntentActivity : AppCompatActivity() {

    private val TAG = "ShareLinkIntentActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        val title: String = bundle?.getString("android.intent.extra.SUBJECT")?.trimForTitle() ?: ""
        val sharedLink: String = bundle?.getString("android.intent.extra.TEXT") ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("YOUTUBE_TITLE", title)
            putExtra("YOUTUBE_LINK", sharedLink)
        }
        startActivity(intent)
        finish()
    }

    private fun String.trimForTitle(): String {
        val len = this.length
        return this.substring(7, len - 12)
    }
}
