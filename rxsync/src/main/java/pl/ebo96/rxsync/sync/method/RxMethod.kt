package pl.ebo96.rxsync.sync.method

import io.reactivex.Flowable
import io.reactivex.functions.Function
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.module.RxModule

class RxMethod<T : Any> private constructor(val async: Boolean, private val retryAttempts: Long, private val delayInMillis: Long) : MethodInfo {

    private val id: Int by lazy { rxExecutorStateStore.generateMethodId() }
    private lateinit var operation: Flowable<MethodResult<out T>>
    private lateinit var module: RxModule<*>
    private var payload: Any? = null
    private var rxMethodEventHandler: RxMethodEventHandler? = null
    private lateinit var rxExecutorStateStore: RxExecutorStateStore
    private lateinit var rxRetryStrategy: RxRetryStrategy<T>


    internal fun getOperation(module: RxModule<*>, rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Flowable<MethodResult<out T>> {
        this.module = module
        this.rxMethodEventHandler = rxMethodEventHandler
        this.rxExecutorStateStore = rxExecutorStateStore
        this.rxRetryStrategy = RxRetryStrategy(rxMethodEventHandler, retryAttempts, delayInMillis)
        return operation
    }

    fun registerOperation(operation: Flowable<T>): RxMethod<T> {
        this.operation = mapToMethodResult(operation)
        this.operation = prepareOperation(this.operation)
        return this
    }

    fun registerOperation(getOperation: () -> T): RxMethod<T> {
        val operation = Flowable.fromCallable { getOperation() }
        registerOperation(operation)
        return this
    }

    fun registerOperationDeferred(getOperation: () -> Flowable<T>): RxMethod<T> {
        registerOperation(Flowable.fromCallable { getOperation() }.flatMap { it })
        return this
    }

    fun withPayload(payload: Any?): RxMethod<T> {
        this.payload = payload
        return this
    }

    /**
     * Need to check if operation 'isEmpty'. If operation return empty(), then it immediately
     * returns onComplete callback which is bad because our 'onNext' method is responsible for
     * counting done methods and final result done method are wrong by that. We can wrap result and return 'null'.
     */
    private fun mapToMethodResult(operation: Flowable<out T>): Flowable<MethodResult<out T>> {
        return Flowable
                .fromCallable {
                    val operationIsEmpty = operation.isEmpty.onErrorReturn { false }.blockingGet()
                    if (operationIsEmpty) {
                        Flowable.just(MethodResult(this@RxMethod, null, payload, module))
                    } else {
                        operation.flatMap { methodResult ->
                            Flowable.just(MethodResult(this@RxMethod, methodResult, payload, module))
                        }
                    }
                }
                .flatMap {
                    it
                }

//        return operation.map { result ->
//            MethodResult(this@RxMethod, result, payload, module)
//        }
    }

    fun doSomethingWithResult(result: (T?) -> Unit): RxMethod<T> {
        this.operation = this.operation.map {
            result(it.result)
            it
        }
        return this
    }

    private fun prepareOperation(operation: Flowable<MethodResult<out T>>): Flowable<MethodResult<out T>> {
        return if (async) {
            prepareAsyncOperation(operation)
        } else {
            prepareSyncOperation(operation)
        }
    }

    private fun prepareSyncOperation(operation: Flowable<MethodResult<out T>>): Flowable<MethodResult<out T>> {
        return operation
                .retryWhen { error -> rxRetryStrategy.create(error) }
                .onErrorResumeNext(Function { error ->
                    if (error is Abort) {
                        Flowable.error(error)
                    } else {
                        Flowable.empty<MethodResult<T>>()
                    }
                })
    }

    private fun prepareAsyncOperation(operation: Flowable<MethodResult<out T>>): Flowable<MethodResult<out T>> {
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