package pl.ebo96.rxsync.sync.module

import io.reactivex.Flowable
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.executor.RxMethodsExecutor
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.method.RxRetryStrategy

/**
 * Modules is object which contains method to execute in defined order if methods are synchronous.
 * For asynchronous methods order isn't guarantee
 *
 * Module can be marked as 'deferred' which means that such module will be executed after not deferred ones
 */
class RxModule<T : Any> private constructor(private val id: Int,
                                            private val rxMethodsExecutor: RxMethodsExecutor<out T>,
                                            private val maxThreads: Int,
                                            private val deferred: Boolean)
    : ModuleInfo, Comparable<RxModule<T>> {

    fun prepareMethods(rxMethodEventHandler: RxMethodEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Flowable<out MethodResult<out T>> {
        return rxMethodsExecutor.prepare(this@RxModule, rxMethodEventHandler, rxExecutorStateStore, maxThreads)
    }

    override fun getModuleId(): Int {
        return id
    }

    override fun getMethodsCount(): Int {
        return rxMethodsExecutor.methodsCount()
    }

    override fun removeModuleMethods() {
        rxMethodsExecutor.removeMethods()
    }

    override fun isDeferred(): Boolean {
        return deferred
    }

    override fun equals(other: Any?): Boolean {
        val module = other as? RxModule<*> ?: return false
        return module.getModuleId() == getModuleId()
    }

    override fun hashCode(): Int {
        return getModuleId()
    }

    override fun compareTo(other: RxModule<T>): Int {
        return when {
            other.getModuleId() < getModuleId() -> -1
            other.getModuleId() > getModuleId() -> 1
            else -> 0
        }
    }

    class Builder<T : Any>(private val id: Int, private var maxThreads: Int, private var deferred: Boolean) {

        private val rxMethods = ArrayList<RxMethod<out T>>()
        private var asyncMethodsRetryAttempts = RxRetryStrategy.DEFAULT_RETRY_ATTEMPTS
        private var asyncMethodsAttemptsDelay = RxRetryStrategy.DEFAULT_ATTEMPTS_DELAY_IN_MILLIS

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            return this
        }

        fun setThreadsLimit(limit: Int): Builder<T> {
            if (limit > 0) {
                maxThreads = limit
            }
            return this
        }

        fun asyncMethodsRetryAttempts(attempts: Long): Builder<T> {
            asyncMethodsRetryAttempts = attempts
            return this
        }

        fun asyncMethodsAttemptsDelay(delay: Long): Builder<T> {
            asyncMethodsAttemptsDelay = delay
            return this
        }

        fun build(): RxModule<T> {
            return RxModule(id, RxMethodsExecutor(rxMethods, asyncMethodsRetryAttempts, asyncMethodsAttemptsDelay), maxThreads, deferred)
        }
    }
}