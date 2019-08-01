package pl.ebo96.rxsync.sync.event

import io.reactivex.functions.Consumer

/**
 * Callback where user can decide what do when error occurred.
 * @see RxMethodEvent
 */
class RxMethodEventConsumer(private val consumer: Consumer<RxMethodEvent>) {

    /**
     * Decide what do when error occurred.
     */
    fun onResponse(rxMethodEvent: RxMethodEvent) {
        consumer.accept(rxMethodEvent)
    }
}