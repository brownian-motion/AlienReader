package com.brownian.alienreader.message.reader

import com.brownian.alienreader.message.Fullname
import com.brownian.alienreader.message.ListingId
import com.brownian.alienreader.message.ReadableThing

/**
 * Defines messages specific to the Reader object controlling the Text-to-Speech engine.
 */

/**
 * Requests a single [ReadableThing], so that the reader can read it out loud when it's ready.
 */
data class LoadReadableThingRequestMessage(val fullname: Fullname, val listingId: ListingId)

/**
 * The response to a [LoadReadableThingRequestMessage],
 * encapsulating the possible results from whatever received the request.
 */
sealed class LoadReadableThingResponseMessage {
    data class Success(val content: ReadableThing, val next: Fullname?, val listingId: ListingId) :
        LoadReadableThingResponseMessage()

    data class Error(val fullname: Fullname, val listingId: ListingId, val error: Throwable) :
        LoadReadableThingResponseMessage()
}

/**
 * Commands the receiving object to start reading the [ReadableThing] identified
 * by the given [Fullname], using the given [ListingId] to determine what to read next.
 *
 * Note that while it is expected that the given [Fullname] does identify a [ReadableThing],
 * whoever receives this message should not assume that it is in fact readable.
 */
data class StartReadingHereCommand(val fullname: Fullname, val listingId: ListingId)

/**
 * Sent by the [ReaderCursorNode] to whoever's listening when
 */
data class ReaderEncounteredErrorMessage(val error: Throwable)

class TtsErrorCodeException(val errorCode: Int) :
    Exception("TextToSpeech raised error code $errorCode")