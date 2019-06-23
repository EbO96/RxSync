package pl.ebo96.rxsyncexample.sync.module

import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.executor.RxMethodsExecutor
import pl.ebo96.rxsyncexample.sync.method.MethodResult
import pl.ebo96.rxsyncexample.sync.method.RxMethod

class RxModule<T : Any> private constructor(private val id: Int, private val rxMethodsExecutor: RxMethodsExecutor<out T>) : ModuleInfo,
        Comparable<RxModule<T>> {

    fun prepareMethods(rxEventHandler: RxExecutor.RxEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Observable<out MethodResult<out T>> {
        return rxMethodsExecutor.prepare(rxEventHandler, rxExecutorStateStore)
    }

    override fun getModuleId(): Int {
        return id
    }

    override fun getMethodsCount(): Int {
        return rxMethodsExecutor.methodsCount()
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

    class Builder<T : Any>(private val id: Int) {

        private val rxMethods = ArrayList<RxMethod<out T>>()

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            return this
        }

        fun build(): RxModule<T> {
            return RxModule(id, RxMethodsExecutor(rxMethods))
        }
    }
}