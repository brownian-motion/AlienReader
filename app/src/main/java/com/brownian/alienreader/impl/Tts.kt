package com.brownian.alienreader.impl

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.brownian.alienreader.message.ReadableThing
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer

interface Tts {
    val status: Observable<StatusMessage>

    val input: Consumer<ControlCommand>

    public sealed class ControlCommand {
        object ShutdownCommand : ControlCommand()
        public sealed class PlaybackCommand : ControlCommand() {
            object StopCommand : PlaybackCommand()
            data class EnqueueCommand(val thing: ReadableThing) : PlaybackCommand()
        }
    }

    public sealed class StatusMessage {
        public data class InitializedMessage(val status: Int) : StatusMessage()

        public sealed class PlaybackMessage : StatusMessage() {
            data class StoppedMessage(
                override val utteranceId: String?,
                val interrupted: Boolean
            ) :
                PlaybackMessage()

            data class StartedMessage(override val utteranceId: String?) : PlaybackMessage()
            data class DoneMessage(override val utteranceId: String?) : PlaybackMessage()
            data class ErrorMessage(override val utteranceId: String?, val errorCode: Int) :
                PlaybackMessage()

            abstract val utteranceId: String?
        }
    }
}


public class TtsWrapper(
    context: Context,
    androidScheduler: Scheduler
) : Tts, AutoCloseable {
    private val inputRelay = PublishRelay.create<Tts.ControlCommand>()
    private val outputRelay = PublishRelay.create<Tts.StatusMessage>()
    private val disposables = CompositeDisposable()

    override val input: Consumer<Tts.ControlCommand> = inputRelay
    override val status: Observable<Tts.StatusMessage> = outputRelay.observeOn(androidScheduler)

    init {
        val textToSpeech = TextToSpeech(context) { status ->
            outputRelay.accept(Tts.StatusMessage.InitializedMessage(status))
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) =
                outputRelay.accept(Tts.StatusMessage.PlaybackMessage.StartedMessage(utteranceId))

            override fun onDone(utteranceId: String?) =
                outputRelay.accept(Tts.StatusMessage.PlaybackMessage.DoneMessage(utteranceId))

            override fun onError(utteranceId: String?, errorCode: Int) =
                outputRelay.accept(
                    Tts.StatusMessage.PlaybackMessage.ErrorMessage(
                        utteranceId,
                        errorCode
                    )
                )

            @Deprecated(
                "required by the old Android API", ReplaceWith(
                    "onError(utteranceId, TextToSpeech.ERROR)",
                    "android.speech.tts.TextToSpeech"
                )
            )
            override fun onError(utteranceId: String?) = onError(utteranceId, TextToSpeech.ERROR)

            override fun onStop(utteranceId: String?, interrupted: Boolean) =
                outputRelay.accept(
                    Tts.StatusMessage.PlaybackMessage.StoppedMessage(
                        utteranceId,
                        interrupted
                    )
                )
        })

        disposables.add(inputRelay.subscribe { command ->
            when (command) {
                is Tts.ControlCommand.ShutdownCommand -> {
                    textToSpeech.shutdown()
                }
                is Tts.ControlCommand.PlaybackCommand.StopCommand -> {
                    textToSpeech.stop()
                }
                is Tts.ControlCommand.PlaybackCommand.EnqueueCommand -> {
                    textToSpeech.speak(
                        command.thing.body, TextToSpeech.QUEUE_ADD, Bundle.EMPTY, command.thing.id.fullname
                    )
                }
            }
        })

    }

    override fun close() {
        inputRelay.accept(Tts.ControlCommand.ShutdownCommand)
        disposables.dispose()
    }

}