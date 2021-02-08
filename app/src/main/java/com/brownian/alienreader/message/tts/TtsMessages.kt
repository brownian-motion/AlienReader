package com.brownian.alienreader.message.tts

import com.brownian.alienreader.message.ReadableThing

/**
 * Defines messages sent to (or by) the object/adapter which directly controls
 * the [android.speech.tts.TextToSpeech] engine.
 */


data class ReadCommand(val content: ReadableThing)