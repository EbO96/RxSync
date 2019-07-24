package pl.ebo96.rxsync.sync.method

import android.annotation.SuppressLint
import io.reactivex.Flowable
import io.reactivex.functions.Function
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler

class RxMethod<T : Any> private constructor(val async: Boolean, private val retryAttempts: Long, private val delayInMillis: Long) : MethodInfo {

    private val id: Int by lazy { rxExecutorStateStore.generateMethodId() }
    private lateinit var operation: Flowable<MethodResult<out T>>
    private var payload: Any? = null
    private var rxMethodEventHandler: RxMethodEventHandler? = null
    private lateinit var rxExecutorStateStore: RxExecutorStateStore
    private lateinit var rxRetryStrategy: RxRetryStrategy<T>


    fun getOperation(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Flowable<MethodResult<out T>> {
        this.rxMethodEventHandler = rxMethodEventHandler
        this.rxExecutorStateStore = rxExecutorStateStore
        this.rxRetryStrategy = RxRetryStrategy(rxMethodEventHandler, retryAttempts, delayInMillis)
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

    fun withPayload(payload: Any?): RxMethod<T> {
        this.payload = payload
        return this
    }

    private fun Flowable<out T>.mapToMethodResult(): Flowable<MethodResult<out T>> {
        return flatMap {
            Flowable.just(MethodResult(this@RxMethod, it, payload))
        }
    }

    fun doSomethingWithResult(result: (T?) -> Unit): RxMethod<T> {
        this.operation = this.operation.flatMap {
            result(it.result)
            Flowable.just(it)
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
                .retryWhen { error -> rxRetryStrategy.create(error) }
                .onErrorResumeNext(Function { error ->
                    if (error is Abort) {
                        Flowable.error(error)
                    } else {
                        Flowable.empty<T>()
                    }
                })
    }

    private fun prepareAsyncOperation(operation: Flowable<T>): Flowable<T> {
        return operation
    }

    override fun getMethodId(): Int = id

    class Abort(message: String? = "") : Throwable(message)

    companion object {

        fun <T : Any> create(async: Boolean,
                             retryAttempts: Long = RxRetryStrategy.DEFAULT_RETRY_ATTEMPTS,
                             delayInMillis: Long = RxRetryStrategy.DEFAULT_ATTEMPTS_DELAY_IN_MILLIS): RxMethod<T> {

            return RxMethod(async, retryAttempts, delayInMillis)
        }
    }
}