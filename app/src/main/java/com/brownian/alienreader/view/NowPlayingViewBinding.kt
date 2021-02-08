package com.brownian.alienreader.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brownian.alienreader.R
import com.brownian.alienreader.impl.Reader

open class NowPlayingViewBinding (protected val rootView: ViewGroup){
    protected val recyclerView : RecyclerView = rootView.findViewById(R.id.sentence_list)
    protected val adapter = SentenceAdapter(LayoutInflater.from(rootView.context));
    protected val loading : View = rootView.findViewById(R.id.loading)
    protected val notStarted : View = rootView.findViewById(R.id.not_playing)
    protected val title : View = rootView.findViewById(R.id.now_playing_title)

    init {
        recyclerView.adapter = adapter
    }

    // TODO: respond to the user clicking on a sentence

    open fun render(state : Reader.State) =
        when(state.playState){
            Reader.PlayState.Uninitialized,
            Reader.PlayState.Loading -> {
                loading.visible()
                recyclerView.gone()
                title.gone()
                notStarted.gone()
            }
            Reader.PlayState.NotPlaying -> {
                loading.gone()
                recyclerView.gone()
                title.gone()
                notStarted.visible()
            }
            is Reader.PlayState.NowPlaying -> {
                loading.gone()
                recyclerView.visible()
                title.visible()
                notStarted.gone()

                adapter.items = state.playState.sentences
                adapter.currentlyPlaying = state.playState.currentlyReadingSentence
                adapter.notifyDataSetChanged()
            }
        }

    private fun View.gone() {
        visibility = View.GONE
    }

    private fun View.visible() {
        visibility = View.VISIBLE
    }
}