package pl.ebo96.rxsync.sync.event

import io.reactivex.functions.Consumer

class RxMethodEventConsumer(private val consumer: Consumer<RxMethodEvent>) {

    fun onResponse(rxMethodEvent: RxMethodEvent) {
        consumer.accept(rxMethodEvent)
    }
}