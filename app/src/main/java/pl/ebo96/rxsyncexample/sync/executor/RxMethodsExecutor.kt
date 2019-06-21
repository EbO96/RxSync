package pl.ebo96.rxsyncexample.sync.executor

import android.util.Log
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import pl.ebo96.rxsyncexample.sync.RxMethod

class RxMethodsExecutor<T : Any>(private val methods: ArrayList<RxMethod<out T>>,
                                 private val lifecycle: RxExecutor.Lifecycle?) {

    fun prepare(): Observable<out T> {
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        val syncMethods: List<Observable<out T>> = methodsGroups[NON_ASYNC]
                ?.map { it.operation }
                ?: emptyList()

        val asyncMethods: List<Observable<out T>> = methodsGroups[ASYNC]
                ?.map { it.operation }
                ?: emptyList()

        //Prepare async methods before execution
        val mergedAsyncMethods = Observable.mergeDelayError(asyncMethods)
                .retryWhen {
                    it.flatMap { error ->
                        Observable.create<T> { emitter ->
                            Log.d(RxExecutor.TAG, "______________________________________________________________")
                            if (lifecycle == null) {
                                emitter.onError(error)
                                emitter.onComplete()
                                return@create
                            }

                            if (!emitter.isDisposed) {
                                val event = Consumer<RxMethod.Event> { event ->
                                    @Suppress("UNCHECKED_CAST")
                                    when (event) {
                                        RxMethod.Event.NEXT -> emitter.onError(error)
                                        RxMethod.Event.RETRY -> emitter.onNext(RetryEvent() as T)
                                        RxMethod.Event.CANCEL -> emitter.onError(RxMethod.Abort())
                                    }
                                    emitter.onComplete()
                                }
                                lifecycle.cannotRetry(error, event)
                            }
                        }
                    }
                }
                .onErrorResumeNext(Function {
                    if (it is RxMethod.Abort) {
                        Observable.error(it)
                    } else {
                        Observable.empty()
                    }
                })

        val mergedSyncMethods = Observable.concat(syncMethods)
                .subscribeOn(RxExecutor.SCHEDULER)

        return Observable.concat(mergedSyncMethods, mergedAsyncMethods).doOnSubscribe {
            Log.d(RxExecutor.TAG, "subscribed")
            RxExecutor.Helper.doneMethods.clear()
            RxExecutor.Helper.numberOfMethods.set(asyncMethods.size + syncMethods.size)
        }
    }

    class RetryEvent

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}