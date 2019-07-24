package pl.ebo96.rxsync.sync.method

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsync.sync.event.RxMethodEvent
import pl.ebo96.rxsync.sync.event.RxMethodEventConsumer
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RxRetryStrategy<T>(private val rxMethodEventHandler: RxMethodEventHandler? = null,
                         private val attempts: Long,
                         private val delayMillis: Long) {

    private var retryCount = AtomicInteger()

    private fun canRetryAutomatically(): Boolean {
        return retryCount.get() < attempts
    }

    private fun incrementRetryCount() {
        retryCount.incrementAndGet()
    }

    private fun reset() {
        retryCount.set(0)
    }

    fun create(error: Throwable): Flowable<T> {
        return if (canRetryAutomatically()) {
            retryAutomatically()
        } else {
            reset()
            waitForUserDecisionWhenError(error)
        }
    }

    fun create(error: Flowable<Throwable>): Flowable<T> {
        return error.flatMap { throwable ->
            create(throwable).subscribeOn(Schedulers.computation())
        }
    }

    private fun <T> retryAutomatically(): Flowable<T> {
        return Flowable.create<T>({ emitter ->
            incrementRetryCount()
            emitter.onNext(RetryEvent() as T)
            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun <T> waitForUserDecisionWhenError(error: Throwable): Flowable<T> {
        return Flowable.create(FlowableOnSubscribe<T> { emitter ->

            //To communicate with user 'MethodEventHandler' must not be null
            if (rxMethodEventHandler == null) {
                emitter.onError(error)
                emitter.onComplete()
                return@FlowableOnSubscribe
            }

            //Wait for user decision
            val eventConsumer = Consumer<RxMethodEvent> { event ->
                if (!emitter.isCancelled) {
                    @Suppress("UNCHECKED_CAST")
                    when (event) {
                        RxMethodEvent.NEXT -> emitter.onError(error)
                        RxMethodEvent.RETRY -> emitter.onNext(RetryEvent() as T)
                        RxMethodEvent.CANCEL -> emitter.onError(RxMethod.Abort())
                    }
                    emitter.onComplete()
                }
            }

            val rxMethodEventConsumer = RxMethodEventConsumer(eventConsumer)

            rxMethodEventHandler.onNewRxEvent(error, rxMethodEventConsumer)
        }, BackpressureStrategy.LATEST).subscribeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        const val DEFAULT_RETRY_ATTEMPTS = 3L
        const val DEFAULT_ATTEMPTS_DELAY_IN_MILLIS = 3000L
    }

    private class RetryEvent
}