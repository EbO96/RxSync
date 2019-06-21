package pl.ebo96.rxsyncexample.sync

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.executor.RxMethodsExecutor

class RxMethod<T : Any> private constructor(val async: Boolean, private val retryAttempts: Long) {

    var lifecycle: RxExecutor.Lifecycle? = null

    var id = RxExecutor.Helper.numberOfMethods.incrementAndGet()

    lateinit var operation: Observable<out T>

    fun registerOperation(operation: Observable<T>): RxMethod<T> {
        this.operation = prepareOperation(operation).doAfterNext {
            RxExecutor.Helper.doneMethods[id] = id
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
                .retry { attempts, error ->
                    Log.d(RxExecutor.TAG, "retry sync -> $attempts, ${error.message}")
                    attempts < retryAttempts
                }.retryWhen { observable ->
                    observable.flatMap { error ->
                        Observable.create<T> { emitter ->
                            Log.d(RxExecutor.TAG, "______________________________________________________________")
                            if (lifecycle == null) {
                                emitter.onError(error)
                                emitter.onComplete()
                                return@create
                            }

                            if (!emitter.isDisposed) {
                                val userDecision = Consumer<Event> { event ->
                                    @Suppress("UNCHECKED_CAST")
                                    when (event) {
                                        Event.NEXT -> emitter.onError(error)
                                        Event.RETRY -> emitter.onNext(RxMethodsExecutor.RetryEvent() as T)
                                        Event.CANCEL -> emitter.onError(Abort(error.message))
                                    }
                                }
                                lifecycle?.cannotRetry(error, userDecision)
                            }
                        }
                    }
                }
                .onErrorResumeNext(Function { error ->
                    if (error is RxMethod.Abort) {
                        Observable.error(error)
                    } else {
                        Observable.empty<T>()
                    }
                })
    }

    private fun prepareAsyncOperation(operation: Observable<T>): Observable<T> {
        return operation.subscribeOn(RxExecutor.SCHEDULER).retry { attempts, error ->
            Log.d(RxExecutor.TAG, "retry async -> $attempts, ${error.message}")
            attempts < retryAttempts
        }
    }

    class Abort(message: String? = "") : Throwable(message)

    sealed class Event {
        object CANCEL : Event()
        object NEXT : Event()
        object RETRY : Event()
    }

    companion object {

        private const val DEFAULT_RETRY_ATTEMPTS = 3L

        fun <T : Any> create(async: Boolean, retryAttempts: Long = DEFAULT_RETRY_ATTEMPTS): RxMethod<T> {
            return RxMethod(async, retryAttempts)
        }
    }
}