package com.brownian.alienreader.message;

import androidx.annotation.NonNull;

/**
 * The receiving end of a message channel.
 * <p>
 * Receives messages in a pre-determined thread.
 * This method should not block.
 * <p>
 * This was designed to passively receive messages in order to establish a "push" model
 * of sending messages. Rather than Rust's channels which explicitly request messages,
 * this architecture requires each receiver be registered with a specific thread
 * on which to receive its messages.
 */
interface Receiver<T> {
    fun onReceive(message: T)
}