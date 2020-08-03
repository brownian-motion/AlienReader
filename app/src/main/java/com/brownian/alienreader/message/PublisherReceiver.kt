package com.brownian.alienreader.message

/**
 * Defines the components of a data pipeline styled after Rust's "channels":
 * https://static.rust-lang.org/doc/master/std/sync/mpsc/index.html
 *
 */

/**
 * Defines an object which can route events along pre-specified Sender -> Receiver channels
 * so that the events are received on a desired thread.
 */
interface EventBus {
    /**
     * Creates a channel from the given sender to the given receiver.
     * When the sender publishes a message, those messages will be received
     * by the given receiver in the given thread.
     *
     * Returns a Throwable if the channel cannot be registered, or null on success.
     */
    fun <T> registerChannel(
        sender: Sender<T>,
        receiver: Receiver<T>,
        receiverThread: Thread
    ): Throwable?

    /**
     * Triggers any events queued on the current thread to be dispatched to the associated Receivers.
     */
    fun dispatchQueuedEvents(): Throwable?

    // TODO: consider adding events to pause or shutdown this event bus. Maybe do this in a broader interface?
}