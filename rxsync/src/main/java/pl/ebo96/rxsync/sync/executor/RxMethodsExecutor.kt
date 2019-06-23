package pl.ebo96.rxsync.sync.executor

import io.reactivex.Observable
import io.reactivex.functions.Function
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.method.RxMethod

class RxMethodsExecutor<T : Any> constructor(private val methods: ArrayList<RxMethod<out T>>) {

    fun prepare(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Observable<out MethodResult<out T>> {
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        val syncMethods: List<Observable<out MethodResult<out T>>> = methodsGroups[NON_ASYNC]
                ?.map { it.getOperation(rxMethodEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        val asyncMethods: List<Observable<out MethodResult<out T>>> = methodsGroups[ASYNC]
                ?.map { it.getOperation(rxMethodEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        //Prepare async methods before execution
        val mergedAsyncMethods = Observable.mergeDelayError(asyncMethods)
                .retryWhen { RxMethod.getMethodRetryStrategy<T>(rxMethodEventHandler, it) }
                .onErrorResumeNext(Function {
                    if (it is RxMethod.Abort) {
                        Observable.error(it)
                    } else {
                        Observable.empty()
                    }
                })

        //Prepare sync methods
        val mergedSyncMethods = Observable.concat(syncMethods).subscribeOn(RxExecutor.SCHEDULER)

        return Observable.concat(mergedSyncMethods, mergedAsyncMethods)

    }

    fun methodsCount(): Int = methods.size

    class RetryEvent

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}