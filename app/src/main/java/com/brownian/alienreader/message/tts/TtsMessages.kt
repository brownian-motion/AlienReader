package com.brownian.alienreader.message.tts

import com.brownian.alienreader.message.ReadableThing

/**
 * Defines messages sent to (or by) the object/adapter which directly controls
 * the [android.speech.tts.TextToSpeech] engine.
 */

/**
 * Command sent to the object controlling the [TextToSpeech] object
 * to command it to shutdown the given object and dispose of it.
 */
class TtsShutdownCommand

class TtsInitializedMessage

sealed class TtsProgressMessage {
    data class TtsStoppedMessage(override val utteranceId: String, val interrupted: Boolean) :
        TtsProgressMessage()

    data class TtsStartedMessage(override val utteranceId: String) : TtsProgressMessage()
    data class TtsDoneMessage(override val utteranceId: String) : TtsProgressMessage()
    data class TtsErrorMessage(override val utteranceId: String, val errorCode: Int) :
        TtsProgressMessage()

    abstract val utteranceId: String
}

data class ReadCommand(val content: ReadableThing)