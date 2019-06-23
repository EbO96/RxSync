package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.functions.Function
import pl.ebo96.rxsyncexample.sync.MethodResult
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore

class RxMethodsExecutor<T : Any> constructor(private val methods: ArrayList<RxMethod<out T>>) {

    fun prepare(rxEventHandler: RxExecutor.RxEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Observable<out MethodResult<out T>> {
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        val syncMethods: List<Observable<out MethodResult<out T>>> = methodsGroups[NON_ASYNC]
                ?.map { it.getOperation(rxEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        val asyncMethods: List<Observable<out MethodResult<out T>>> = methodsGroups[ASYNC]
                ?.map { it.getOperation(rxEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        //Prepare async methods before execution
        val mergedAsyncMethods = Observable.mergeDelayError(asyncMethods)
                .retryWhen { RxMethod.getMethodRetryStrategy<T>(rxEventHandler, it) }
                .onErrorResumeNext(Function {
                    if (it is RxMethod.Abort) {
                        Observable.error(it)
                    } else {
                        Observable.empty()
                    }
                })

        //Prepare sync methods
        val mergedSyncMethods = Observable.concat(syncMethods)
                .subscribeOn(RxExecutor.SCHEDULER)

        return Observable.concat(mergedSyncMethods, mergedAsyncMethods)
    }

    fun methodsCount(): Int = methods.size

    class RetryEvent

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}