package pl.ebo96.rxsync.sync.method

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEvent
import pl.ebo96.rxsync.sync.event.RxMethodEventConsumer
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.executor.RxExecutor
import pl.ebo96.rxsync.sync.executor.RxMethodsExecutor

class RxMethod<T : Any> private constructor(val async: Boolean, private val retryAttempts: Long) : MethodInfo {

    private var rxMethodEventHandler: RxMethodEventHandler? = null
    private lateinit var rxExecutorStateStore: RxExecutorStateStore

    private val id: Int by lazy { rxExecutorStateStore.generateMethodId() }

    private lateinit var operation: Flowable<MethodResult<out T>>

    fun getOperation(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Flowable<MethodResult<out T>> {
        this.rxMethodEventHandler = rxMethodEventHandler
        this.rxExecutorStateStore = rxExecutorStateStore
        return operation
    }

    fun registerOperation(operation: Flowable<T>): RxMethod<T> {
        this.operation = prepareOperation(operation).mapToMethodResult()
        return this
    }

    fun registerOperation(getOperation: () -> T): RxMethod<T> {
        val operation = Flowable.fromCallable { getOperation() }
        registerOperation(operation)
        return this
    }

    private fun Flowable<out T>.mapToMethodResult(): Flowable<MethodResult<out T>> {
        return flatMap {
            Flowable.just(MethodResult(this@RxMethod, it)).subscribeOn(RxExecutor.SCHEDULER)
        }
    }

    fun doSomethingWithResult(result: (T?) -> Unit): RxMethod<T> {
        this.operation = this.operation.flatMap {
            result(it.result)
            Flowable.just(it).subscribeOn(RxExecutor.SCHEDULER)
        }
        return this
    }

    @SuppressLint("CheckResult")
    private fun prepareOperation(operation: Flowable<T>): Flowable<out T> {
        return if (async) {
            prepareAsyncOperation(operation)
        } else {
            prepareSyncOperation(operation)
        }
    }

    private fun prepareSyncOperation(operation: Flowable<T>): Flowable<T> {
        return operation
                .subscribeOn(RxExecutor.SCHEDULER)
                .retry { attempts, error ->
                    Log.d(RxExecutor.TAG, "retry sync -> $attempts, ${error.message} on thread -> ${Thread.currentThread().name}")
                    attempts < retryAttempts
                }
                .retryWhen { getMethodRetryStrategy<T>(rxMethodEventHandler, it) }
                .onErrorResumeNext(Function { error ->
                    if (error is Abort) {
                        Flowable.error(error)
                    } else {
                        Flowable.empty<T>()
                    }
                })
    }

    private fun prepareAsyncOperation(operation: Flowable<T>): Flowable<T> {
        return operation.subscribeOn(RxExecutor.SCHEDULER)
                .retry { attempts, error ->
                    Log.d(RxExecutor.TAG, "retry async -> $attempts, ${error.message}")
                    attempts < retryAttempts
                }
    }

    override fun getMethodId(): Int = id

    class Abort(message: String? = "") : Throwable(message)

    companion object {

        private const val DEFAULT_RETRY_ATTEMPTS = 3L

        fun <T : Any> create(async: Boolean, retryAttempts: Long = DEFAULT_RETRY_ATTEMPTS): RxMethod<T> {
            return RxMethod(async, retryAttempts)
        }

        fun <T : Any> getMethodRetryStrategy(rxMethodEventHandler: RxMethodEventHandler?, errorFlowable: Flowable<Throwable>): Flowable<T> {
            return errorFlowable.flatMap { error ->
                Flowable.create(FlowableOnSubscribe<T> { emitter ->
                    Log.d(RxExecutor.TAG, "______________________________________________________________")
                    if (rxMethodEventHandler == null) {
                        emitter.onError(error)
                        emitter.onComplete()
                        return@FlowableOnSubscribe
                    }

                    val eventConsumer = Consumer<RxMethodEvent> { event ->
                        if (!emitter.isCancelled) {
                            @Suppress("UNCHECKED_CAST")
                            when (event) {
                                RxMethodEvent.NEXT -> emitter.onError(error)
                                RxMethodEvent.RETRY -> emitter.onNext(RxMethodsExecutor.RetryEvent() as T)
                                RxMethodEvent.CANCEL -> emitter.onError(Abort())
                            }
                            emitter.onComplete()
                        }
                    }

                    val rxMethodEventConsumer = RxMethodEventConsumer(eventConsumer)

                    rxMethodEventHandler.onNewRxEvent(error, rxMethodEventConsumer)
                }, BackpressureStrategy.LATEST).subscribeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}