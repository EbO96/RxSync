package pl.ebo96.rxsync.sync.executor

import android.annotation.SuppressLint
import io.reactivex.Flowable
import io.reactivex.functions.Function
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.method.RxMethod

class RxMethodsExecutor<T : Any> constructor(private val methods: ArrayList<RxMethod<out T>>) {

    @SuppressLint("CheckResult")
    fun prepare(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Flowable<out MethodResult<out T>> {
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        val syncMethods: List<Flowable<out MethodResult<out T>>> = methodsGroups[NON_ASYNC]
                ?.map { it.getOperation(rxMethodEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        val asyncMethods: List<Flowable<out MethodResult<out T>>> = methodsGroups[ASYNC]
                ?.map { it.getOperation(rxMethodEventHandler, rxExecutorStateStore) }
                ?: emptyList()

        //Prepare async methods before execution
        val mergedAsyncMethods = Flowable.mergeDelayError(asyncMethods)
                .retryWhen { RxMethod.getMethodRetryStrategy<T>(rxMethodEventHandler, it) }
                .onErrorResumeNext(Function {
                    if (it is RxMethod.Abort) {
                        Flowable.error(it)
                    } else {
                        Flowable.empty()
                    }
                })

        //Prepare sync methods
        val mergedSyncMethods = Flowable.concat(syncMethods).subscribeOn(RxExecutor.SCHEDULER)

        return Flowable.concat(mergedSyncMethods, mergedAsyncMethods)

    }

    fun methodsCount(): Int = methods.size

    class RetryEvent

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}