package com.brownian.alienreader.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.brownian.alienreader.impl.Reader
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer

class NowPlayingViewModel (
    reader: Reader,
    androidScheduler: Scheduler
        ) : ViewModel() {

    private val inputRelay = PublishRelay.create<Reader.Action>()
    private val mutableState = MutableLiveData<Reader.State>()
    private val disposables = CompositeDisposable()

    // our public API:
    val input: Consumer<Reader.Action> = inputRelay
    val state: LiveData<Reader.State> = mutableState

    init {
        disposables.add(inputRelay.subscribe(reader.input))
        disposables.add(
            reader.state
                .observeOn(androidScheduler)
                .subscribe(mutableState::setValue)
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}