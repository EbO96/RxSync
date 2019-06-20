package pl.ebo96.rxsyncexample.sync

import android.annotation.SuppressLint
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class RxMethod<T : Any> private constructor(val async: Boolean, private val retryAttempts: Long, private val scheduler: Scheduler) {

    val id = RxExecutor.Helper.numberOfMethods

    var lifecycle: RxExecutor.Lifecycle? = null

    init {
        RxExecutor.Helper.numberOfMethods++
    }

    private lateinit var operation: Observable<T>

    fun registerOperation(operation: Observable<T>): RxMethod<T> {
        this.operation = operation
        return this
    }

    fun join(operation: (T) -> RxMethod<out T>): RxMethod<T> {
        this.operation = this.operation.flatMap { operation(it).start() }
        return this
    }

    fun modifyResult(map: (T) -> T): RxMethod<out T> {
        operation = operation.flatMap {
            Observable.just(map(it))
        }
        return this
    }

    fun doSomethingWithResult(modify: (T) -> Unit): RxMethod<T> {
        operation = operation.flatMap {
            modify(it)
            Observable.just(it)
        }
        return this
    }

    @SuppressLint("CheckResult")
    fun start(): Observable<out T> {
        this.operation = when (async) {
            true -> operation.subscribeOn(scheduler)
            else -> operation
        }

        this.operation = when (retryAttempts > 0) {
            true -> this.operation.retry(retryAttempts).onErrorResumeNext(::handleFatalError)
            else -> this.operation
        }

        this.operation = this.operation.observeOn(AndroidSchedulers.mainThread())

        return operation
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleFatalError(error: Throwable): Observable<out T> = Observable.create<T> { emitter ->
        val userDecision = Consumer<ErrorEvent> { event ->
            if (!emitter.isDisposed) {
                if (event.resume) {
                    emitter.onNext((event.with ?: SyncResume()) as T)
                    emitter.onComplete()
                } else {
                    emitter.onError(error)
                    emitter.onComplete()
                }
            }
        }

        lifecycle?.cannotRetry(error, userDecision)
    }.subscribeOn(AndroidSchedulers.mainThread())

    class SyncResume

    companion object {

        private const val DEFAULT_RETRY_ATTEMPTS = 3L

        fun <T : Any> create(async: Boolean, retryAttempts: Long = DEFAULT_RETRY_ATTEMPTS, scheduler: Scheduler = Schedulers.io()): RxMethod<T> {
            return RxMethod(async, retryAttempts, scheduler)
        }
    }
}