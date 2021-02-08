package com.brownian.alienreader.message;

import androidx.annotation.Nullable;

/**
 * The sending end of a message channel.
 * <p>
 * You send a message to something else by sending it here.
 * <p>
 * Returns a Throwable if the channel cannot be registered, or null on success.
 */
interface Sender<T> {
    fun send(message: T): Throwable?
}