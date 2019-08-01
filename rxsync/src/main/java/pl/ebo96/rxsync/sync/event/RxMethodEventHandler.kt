package pl.ebo96.rxsync.sync.event

/**
 *
 */
interface RxMethodEventHandler {

    /**
     *
     */
    fun onNewRxEvent(error: Throwable, rxMethodEvent: RxMethodEventConsumer)
}