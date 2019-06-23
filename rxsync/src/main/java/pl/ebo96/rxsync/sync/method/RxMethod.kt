package pl.ebo96.rxsync.sync.method

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.Observable
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

    private lateinit var operation: Observable<MethodResult<out T>>

    fun getOperation(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Observable<MethodResult<out T>> {
        this.rxMethodEventHandler = rxMethodEventHandler
        this.rxExecutorStateStore = rxExecutorStateStore
        return operation
    }

    fun registerOperation(operation: Observable<T>): RxMethod<T> {
        this.operation = prepareOperation(operation).mapToMethodResult()
        return this
    }

    fun registerOperation(getOperation: () -> T): RxMethod<T> {
        val operation = Observable.fromCallable { getOperation() }
        registerOperation(operation)
        return this
    }

    private fun Observable<out T>.mapToMethodResult(): Observable<MethodResult<out T>> {
        return flatMap {
            Observable.just(MethodResult(this@RxMethod, it)).subscribeOn(RxExecutor.SCHEDULER)
        }
    }

    fun doSomethingWithResult(result: (T?) -> Unit): RxMethod<T> {
        this.operation = this.operation.flatMap {
            result(it.result)
            Observable.just(it).subscribeOn(RxExecutor.SCHEDULER)
        }
        return this
    }

    @SuppressLint("CheckResult")
    private fun prepareOperation(operation: Observable<T>): Observable<out T> {
        return if (async) {
            prepareAsyncOperation(operation)
        } else {
            prepareSyncOperation(operation)
        }
    }

    private fun prepareSyncOperation(operation: Observable<T>): Observable<T> {
        return operation
                .subscribeOn(RxExecutor.SCHEDULER)
                .retry { attempts, error ->
                    Log.d(RxExecutor.TAG, "retry sync -> $attempts, ${error.message} on thread -> ${Thread.currentThread().name}")
                    attempts < retryAttempts
                }
                .retryWhen { getMethodRetryStrategy<T>(rxMethodEventHandler, it) }
                .onErrorResumeNext(Function { error ->
                    if (error is Abort) {
                        Observable.error(error)
                    } else {
                        Observable.empty<T>()
                    }
                })
    }

    private fun prepareAsyncOperation(operation: Observable<T>): Observable<T> {
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

        fun <T : Any> getMethodRetryStrategy(rxMethodEventHandler: RxMethodEventHandler?, errorObservable: Observable<Throwable>): Observable<T> {
            return errorObservable.flatMap { error ->
                Observable.create<T> { emitter ->
                    Log.d(RxExecutor.TAG, "______________________________________________________________")
                    if (rxMethodEventHandler == null) {
                        emitter.onError(error)
                        emitter.onComplete()
                        return@create
                    }

                    val eventConsumer = Consumer<RxMethodEvent> { event ->
                        if (!emitter.isDisposed) {
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
                }.subscribeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}