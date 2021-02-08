package com.brownian.alienreader.impl

import com.brownian.alienreader.message.Fullname
import com.brownian.alienreader.message.ListingId
import com.brownian.alienreader.message.ReadableThing
import com.freeletics.rxredux.Reducer
import com.freeletics.rxredux.SideEffect
import com.freeletics.rxredux.reduxStore
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class Reader(private val minBufferSize: Byte = 3) {
    data class State(
        val playState: PlayState,
        val playQueue: PlayQueue
    )

    sealed class PlayState {
        object Uninitialized : PlayState() // for when the TTS engine isn't ready yet
        object NotPlaying :
            PlayState() // for when the user hasn't told us what to play yet, or just re-opened the app and shouldn't start the play queue yet

        object Loading :
            PlayState() // for when we WANT to play but we're waiting on the buffer to load

        data class NowPlaying(
            val sentences: List<ReadableThing>,
            val post: ReadableThing,
            val currentlyReadingSentence: Int,
            val paused: Boolean
        ) : PlayState() {
            fun currentSentence(): ReadableThing = sentences[currentlyReadingSentence]
        }

        fun paused(): PlayState = when (this) {
            is NowPlaying -> this.copy(paused = true)
            else -> this
        }
    }

    data class PlayQueue(
        val queued: List<ReadableThing>,
        val next: Pair<Fullname, ListingId>?
    )

    public sealed class Action {

        /**
         * Command sent to the object controlling the [TextToSpeech] object
         * to command it to shutdown the given object and dispose of it.
         */
        object TtsShutdownCommand : Action()

        object TtsInitializedMessage : Action()

        sealed class TtsProgressMessage : Action() {
            data class TtsStoppedMessage(
                override val utteranceId: String,
                val interrupted: Boolean
            ) :
                TtsProgressMessage()

            data class TtsStartedMessage(override val utteranceId: String) : TtsProgressMessage()
            data class TtsDoneMessage(override val utteranceId: String) : TtsProgressMessage()
            data class TtsErrorMessage(override val utteranceId: String, val errorCode: Int) :
                TtsProgressMessage()

            abstract val utteranceId: String
        }

        data class StartPlayingFromSourceCommand(
            val listingId: ListingId
        ) : Action()

        data class AddToPlayQueueMessage(val thing: ReadableThing) :
            Action() // TODO: make this varargs?

        object PlayNextInQueueCommand : Action()

        object StartPlayingCommand : Action()
    }

    private data class ReadableCursor(
        val content: ReadableThing,
        val listingId: ListingId,
        val next: Fullname?
    )

    val input: Relay<Action> = PublishRelay.create()

    // We're gonna assert that this object receives every event in the same thread,
    // so that we can assume that the state doesn't change DURING a function.
    val state: Observable<State> = input
        .reduxStore(
            initialState = State(PlayState.Uninitialized, PlayQueue(emptyList(), null)),
            sideEffects = listOf<SideEffect<State, Action>>(
                // TODO: maintain buffer
                ::listenToStartPlayingFromSource,
                ::playNextOnAddToEmptyPlayQueue,
//                ::sendToTtsOnPlayNext
            ),
            reducer = ::reducer as Reducer<State, Action>
        )
        .distinctUntilChanged()


    private fun sendToTtsOnPlayNext(
        actions: Observable<Action>,
        stateGetter: () -> State
    ): Observable<out Action> = actions
        .ofType(Action.PlayNextInQueueCommand::class.java)
        .filter {
            // this side effect will get run RIGHT after the play state is updated, so we can pull from there
            stateGetter.invoke().playState is PlayState.NowPlaying
        }
        .map {
            // this side effect will get run RIGHT after the play state is updated, so we can pull from there
            TODO("Send message to TTS")
        }

    private fun playNextOnAddToEmptyPlayQueue(
        actions: Observable<Action>,
        stateGetter: () -> State
    ): Observable<out Action> = actions
        .ofType(Action.AddToPlayQueueMessage::class.java)
        .filter { stateGetter.invoke().playState is PlayState.Loading }
        .map { Action.PlayNextInQueueCommand }

    private fun listenToStartPlayingFromSource(
        actions: Observable<Action>,
        stateGetter: () -> State
    ): Observable<out Action> = actions
        .ofType(Action.StartPlayingFromSourceCommand::class.java)
        .flatMap {
            Observable.fromArray(
                Action.StartPlayingCommand,
                Action.AddToPlayQueueMessage(
                    ReadableThing(
                        Fullname("FAKE_FULLNAME"),
                        "Fake Listing"
                    )
                )
            )
        }
        .delay(
            1,
            TimeUnit.SECONDS
        ) // add some delay to make the loading indicator appear JUST DURING PROTOTYPING


    fun reducer(state: State, action: Action): State = when (action) {
        is Action.TtsProgressMessage -> {
            onTtsProgress(state, action)
        }
        is Action.TtsInitializedMessage -> state.copy(playState = Reader.PlayState.NotPlaying)
        is Action.AddToPlayQueueMessage -> state.copy(
            playQueue = state.playQueue.copy(
                queued = state.playQueue.queued.plus(
                    action.thing
                )
            )
        )
        is Action.TtsProgressMessage.TtsDoneMessage -> playNextSentence(state)
        is Action.PlayNextInQueueCommand -> playNextPost(state)
        is Action.StartPlayingCommand -> {
            when(state.playState) {
                is PlayState.NotPlaying -> state.copy(playState = PlayState.Loading)
                is PlayState.NowPlaying -> state.copy(playState = state.playState.copy(paused = false))
                else -> state
            }
        }
        else -> {
            // TODO
            state
        }
    }

    // Interactions between the ReaderCursorNode and the TtsControllerNode:
    private fun onTtsProgress(state: State, message: Action.TtsProgressMessage): State {
        return when (message) {
            is Action.TtsProgressMessage.TtsDoneMessage -> playNextSentence(state)
            is Action.TtsProgressMessage.TtsErrorMessage -> {
//                readerEncounteredErrorMessageSender.send(
//                    ReaderEncounteredErrorMessage(
//                        TtsErrorCodeException(message.errorCode)
//                    )
//                )
                if (state.playState is Reader.PlayState.NowPlaying) {
                    state.copy(playState = state.playState.copy(paused = true))
                } else {
                    state
                }
            }
            is Action.TtsProgressMessage.TtsStartedMessage -> {
                val utteranceId = message.utteranceId

                if (state.playState !is Reader.PlayState.NowPlaying) {
                    error("Expected to only receive \"TtsStartedMessage\" while in the Reading state")
                }

                state

//                TODO: consider figuring out how to respond here?
            }
            is Action.TtsProgressMessage.TtsStoppedMessage -> state.copy(playState = state.playState.paused())
        }
    }

    private fun playNextSentence(currentState: State): State {
        val (playState, queue) = currentState

        if (queue.queued.size < minBufferSize && queue.next != null) {
            // request more things to read for later
//            loadReadableThingRequestSender.send(
//                LoadReadableThingRequestMessage(
//                    queue.next.first,
//                    queue.next.second
//                )
//            )
        }

        return if (playState is Reader.PlayState.NowPlaying && playState.currentlyReadingSentence < playState.sentences.size - 1) {
            // if we we still have sentences, then we don't need to advance to the next post, just the next sentence
            val nextPlayState =
                playState.copy(currentlyReadingSentence = playState.currentlyReadingSentence + 1)
//            readCommandSender.send(ReadCommand(nextPlayState.currentSentence()))
            currentState.copy(playState = nextPlayState)
        } else playNextPost(currentState)
    }

    // used in response to finishing a post or a user skipping to the next one
    private fun playNextPost(currentState: State): State {
        val (_, queue) = currentState

        return when {
            queue.queued.isEmpty() -> {
                if (queue.next == null)
                // because we're done with the queue
                    currentState.copy(playState = Reader.PlayState.NotPlaying)
                else
                // because we've asked for more, but we don't have it yet
                    currentState.copy(playState = Reader.PlayState.Loading)

            }
            else -> {
                val (postToRead, rest) = queue.queued.removeFirst()
                val nowPlaying = Reader.PlayState.NowPlaying(
                    getSentences(postToRead),
                    postToRead,
                    0,
                    paused = false
                )
//                readCommandSender.send(ReadCommand(postToRead))
                State(nowPlaying, queue.copy(queued = rest))
            }
        }
    }

    private fun <T> List<T>.removeFirst(): Pair<T, List<T>> =
        Pair(this[0], this.subList(1, this.size))

    private fun getSentences(next: ReadableThing): List<ReadableThing> =
        next.body.split(Regex("\\.\\s+")).map { ReadableThing(next.id, it) } .toList()
}