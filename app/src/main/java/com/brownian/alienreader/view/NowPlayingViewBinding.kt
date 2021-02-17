package com.brownian.alienreader.view

import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.brownian.alienreader.R
import com.brownian.alienreader.impl.Reader

open class NowPlayingViewBinding(private val rootView: ViewGroup) {
    protected val body: TextView = rootView.findViewById(R.id.now_playing_body)
    protected val loading: View = rootView.findViewById(R.id.loading)
    protected val notStarted: View = rootView.findViewById(R.id.not_playing)
    protected val title: TextView = rootView.findViewById(R.id.now_playing_title)

    // TODO: respond to the user clicking on a sentence

    open fun render(state: Reader.State) =
        when (state.playState) {
            Reader.PlayState.Uninitialized,
            Reader.PlayState.Loading -> {
                loading.visible()
                body.gone()
                title.gone()
                notStarted.gone()
            }
            Reader.PlayState.NotPlaying -> {
                loading.gone()
                body.gone()
                title.gone()
                notStarted.visible()
            }
            is Reader.PlayState.NowPlaying -> {
                loading.gone()
                body.visible()
                title.visible()
                notStarted.gone()

                title.text = "Playing " + state.playState.post.id
                body.text = formatBody(state.playState)
            }
        }

    private fun formatBody(playState: Reader.PlayState.NowPlaying): CharSequence {
        var front = playState.sentences.subList(0, playState.currentlyReadingSentence)
        var reading = playState.currentSentence()
        var back = playState.sentences.subList(
            playState.currentlyReadingSentence + 1,
            playState.sentences.size
        )

        var text = SpannableStringBuilder()

        text.appendLine(front.map { it.body }.joinToString(separator = "  "))

        text.appendLine(reading.body) // TODO make this bold

        text.append(back.map { it.body }.joinToString("  "))

        return text
    }

    private fun View.gone() {
        visibility = View.GONE
    }

    private fun View.visible() {
        visibility = View.VISIBLE
    }
}