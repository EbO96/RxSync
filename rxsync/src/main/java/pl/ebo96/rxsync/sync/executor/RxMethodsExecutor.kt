package pl.ebo96.rxsync.sync.executor

import android.annotation.SuppressLint
import io.reactivex.Flowable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.method.RxRetryStrategy
import pl.ebo96.rxsync.sync.module.RxModule

/**
 * Execute module methods. Methods are grouped at two group.
 * First group represents methods which will executed synchronously, one by one.
 * Second group contains asynchronous methods which will executed asynchronously.
 *
 * @param methods synchronous and asynchronous methods
 */
class RxMethodsExecutor<T : Any>(private val methods: ArrayList<RxMethod<out T>>,
                                 private val asyncMethodsRetryAttempts: Long,
                                 private val asyncMethodsAttemptsDelay: Long) {

    @SuppressLint("CheckResult")
    fun prepare(module: RxModule<*>, rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore, maxThreads: Int): Flowable<out MethodResult<out T>> {

        //Group methods
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        //Create schedulers for asynchronous methods
        val scheduler = RxScheduler.create(maxThreads)

        //Prepare synchronous methods
        val syncMethods: List<Flowable<out MethodResult<out T>>> = methodsGroups[NON_ASYNC]
                ?.map { rxMethod ->
                    rxMethod.getOperation(module, rxMethodEventHandler, rxExecutorStateStore).subscribeOn(scheduler)
                }
                ?: emptyList()

        //Prepare asynchronous methods and subscribe every method on previously created scheduler.
        //Scheduler can limit number of threads used
        val asyncMethods: List<Flowable<out MethodResult<out T>>> = methodsGroups[ASYNC]
                ?.map { rxMethod ->
                    rxMethod.getOperation(module, rxMethodEventHandler, rxExecutorStateStore).subscribeOn(scheduler)
                }
                ?: emptyList()

        //Prepare async methods before execution
        val asynchronousOperations = Flowable.mergeDelayError(asyncMethods)
                .parallel(maxThreads)
                .runOn(Schedulers.computation())
                .sequential(maxThreads)
                .retryWhen { error: Flowable<Throwable> ->
                    RxRetryStrategy<T>(
                            rxMethodEventHandler,
                            asyncMethodsRetryAttempts,
                            asyncMethodsAttemptsDelay
                    ).create(error)
                }
                .onErrorResumeNext(Function { error ->
                    if (error is RxMethod.Abort) {
                        Flowable.error(error)
                    } else {
                        Flowable.empty()
                    }
                })

        //Execute synchronous and next asynchronous methods. Wait for all methods and go to next module
        return Flowable
                .concat(
                        //First execute all synchronous methods one by one
                        Flowable.concat(syncMethods),
                        //Next, execute all asynchronous methods
                        asynchronousOperations
                )
    }

    fun methodsCount(): Int = methods.size

    fun removeMethods() {
        methods.clear()
    }

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}