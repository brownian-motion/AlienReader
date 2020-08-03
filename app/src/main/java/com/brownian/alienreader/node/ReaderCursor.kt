package com.brownian.alienreader.node

import com.brownian.alienreader.BuildConfig
import com.brownian.alienreader.message.*
import com.brownian.alienreader.message.reader.*
import com.brownian.alienreader.message.tts.ReadCommand
import com.brownian.alienreader.message.tts.TtsInitializedMessage
import com.brownian.alienreader.message.tts.TtsProgressMessage
import com.brownian.alienreader.message.tts.TtsShutdownCommand

interface ReaderCursorNode {
    // Interactions between the ReaderCursorNode and the TtsControllerNode:
    val ttsProgressListener: Receiver<TtsProgressMessage>
    val ttsInitListener: Receiver<TtsInitializedMessage>
    val ttsShutdownCommandSender: Sender<TtsShutdownCommand>
    val readCommandSender: Sender<ReadCommand>

    // Interactions between the ReaderCursorNode and the ListingManagerNode:
    val loadReadableThingResponseReceiver: Receiver<LoadReadableThingResponseMessage>
    val loadReadableThingRequestSender: Sender<LoadReadableThingRequestMessage>

    // Interactions between the ReaderCursorNode and activities that control it:
    val startReadingHereListener: Receiver<StartReadingHereCommand>
    val mediaCommandListener: Receiver<MediaPlayerCommand>
    val readerEncounteredErrorMessageSender: Sender<ReaderEncounteredErrorMessage>
}

class ReaderCursorNodeImpl : ReaderCursorNode {
    private sealed class State {
        data class Uninitialized(val playThisWhenReady: ReadableCursor?) : State()
        object Stopped : State()
        data class Reading(
            val nowPlaying: ReadableCursor,
            val paused: Boolean,
            /**
             * Stores a small cache of the things to read next, with notes on how to get the next item.
             * When this is empty, this object will try to request more things using the current cursor.
             * <p>
             * Elements should only be added on the same thread that receives all events,
             * so if we stick to that design then we don't need to worry about thread safety here.
             * <p>
             * TODO I know this isn't the best model of the play queue. But it's good enough for now.
             */
            val next: ReadableCursor?
        ) : State()
    }

    private data class ReadableCursor(
        val content: ReadableThing,
        val listingId: ListingId,
        val next: Fullname?
    )

    // We're gonna assert that this object receives every event in the same thread,
    // so that we can assume that the state doesn't change DURING a function.
    private var state: State = State.Uninitialized(null)

    // Interactions between the ReaderCursorNode and the TtsControllerNode:
    override val ttsProgressListener = Receiver<TtsProgressMessage> { message ->
        val currentState = state
        state = when (message) {
            is TtsProgressMessage.TtsDoneMessage -> {
                if (currentState is State.Reading) {
                    playNextInQueue(currentState)
                } else {
                    State.Stopped
                }
            }
            is TtsProgressMessage.TtsErrorMessage -> {
                readerEncounteredErrorMessageSender.send(
                    ReaderEncounteredErrorMessage(
                        TtsErrorCodeException(message.errorCode)
                    )
                )
                if (currentState is State.Reading) {
                    State.Reading(currentState.nowPlaying, false, currentState.next)
                } else {
                    currentState
                }
            }
            is TtsProgressMessage.TtsStartedMessage -> {
                val utteranceId = message.utteranceId

                if (BuildConfig.DEBUG && currentState !is State.Reading) {
                    error("Expected to only receive \"TtsStartedMessage\" while in the Reading state")
                }

                val (nowPlaying, _, next) = currentState as State.Reading

                if (BuildConfig.DEBUG && nowPlaying.content.id.fullname != utteranceId) {
                    error("The reader just started playing something other than what we're tracking as \"Now Playing\" (expected ${nowPlaying.content.id.fullname}, but started $utteranceId")
                }

                State.Reading(nowPlaying = nowPlaying, paused = false, next = next)
            }
            is TtsProgressMessage.TtsStoppedMessage -> State.Stopped
        }
    }

    private fun playNextInQueue(currentState: State.Reading): State {
        val (nowPlaying, _, next) = currentState
        if (next == null) {
            if (nowPlaying.next == null) {
                return State.Stopped
            }
            loadReadableThingRequestSender.send(
                LoadReadableThingRequestMessage(
                    nowPlaying.next,
                    nowPlaying.listingId
                )
            )
            return State.Stopped
        } else {
            if (next.next != null) {
                loadReadableThingRequestSender.send(
                    LoadReadableThingRequestMessage(
                        next.next,
                        nowPlaying.listingId
                    )
                )
            }
            readCommandSender.send(ReadCommand(next.content))
            return State.Reading(next, false, null)
        }
    }

    override val ttsInitListener = Receiver<TtsInitializedMessage> { state = State.Stopped }
    override lateinit var ttsShutdownCommandSender: Sender<TtsShutdownCommand>
    override lateinit var readCommandSender: Sender<ReadCommand>

    // Interactions between the ReaderCursorNode and the ListingManagerNode:
    override val loadReadableThingResponseReceiver =
        Receiver<LoadReadableThingResponseMessage> { message ->
            if (message is LoadReadableThingResponseMessage.Error) {
                readerEncounteredErrorMessageSender.send(ReaderEncounteredErrorMessage(message.error))
                // no state change ; we'll let the activity managing this try again
                return@Receiver
            }

            val currentState = state
            val message = message as LoadReadableThingResponseMessage.Success
            val newContent = ReadableCursor(message.content, message.listingId, message.next)
            state = when (currentState) {
                is State.Uninitialized -> {
                    State.Uninitialized(newContent)
                }
                is State.Stopped -> {
                    readCommandSender.send(ReadCommand(newContent.content))
                    State.Reading(newContent, true, null)
                }
                is State.Reading -> {
                    State.Reading(currentState.nowPlaying, currentState.paused, newContent)
                }
            }
        }


    override lateinit var loadReadableThingRequestSender: Sender<LoadReadableThingRequestMessage>

    // Interactions between the ReaderCursorNode and activities that control it:
    override val startReadingHereListener = Receiver<StartReadingHereCommand> { message ->
        loadReadableThingRequestSender.send(
            LoadReadableThingRequestMessage(
                message.fullname,
                message.listingId
            )
        )
        state = if (state is State.Uninitialized) State.Uninitialized(null) else State.Stopped
    }
    override val mediaCommandListener = Receiver<MediaPlayerCommand> { message ->
        TODO("Not implemented yet, I need to find a way to forward these to the TtsControllerNode")
    }
    override lateinit var readerEncounteredErrorMessageSender: Sender<ReaderEncounteredErrorMessage>
}