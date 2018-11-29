package com.example.boris.musicdownloader.presenters

import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Test

class DownloadFragmentPresenterImplTest {

    private var presenter: DiscoverFragmentPresenterImpl = DiscoverFragmentPresenterImpl(mock())

    @Test
    fun `link validity and extract token for type1`() {
        val link = "http://www.youtube.com/watch?v=iwGFalTRHDA"

        val validity = presenter.checkValidYoutubeUri(link)
        Assert.assertTrue(validity)

        val token = presenter.extractYoutubeId(link)
        Assert.assertEquals("iwGFalTRHDA", token)
    }

    @Test
    fun `link validity and extract token for type2`() {
        val link = "http://www.youtube.com/watch?v=iwGFalTRHDA&feature=related"

        val validity = presenter.checkValidYoutubeUri(link)
        Assert.assertTrue(validity)

        val token = presenter.extractYoutubeId(link)
        Assert.assertEquals("iwGFalTRHDA", token)
    }

    @Test
    fun `link validity and extract token for type3`() {
        val link = "http://youtu.be/iwGFalTRHDA"

        val validity = presenter.checkValidYoutubeUri(link)
        Assert.assertTrue(validity)

        val token = presenter.extractYoutubeId(link)
        Assert.assertEquals("iwGFalTRHDA", token)
    }
}