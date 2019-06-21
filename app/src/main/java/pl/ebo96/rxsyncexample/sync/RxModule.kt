package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.executor.RxMethodsExecutor

class RxModule<T : Any> private constructor(private val rxMethodsExecutor: RxMethodsExecutor<out T>) {

    fun prepareMethods(): Observable<out T> {
        return rxMethodsExecutor.prepare()
    }

    class Builder<T : Any>(private val lifecycle: RxExecutor.Lifecycle?) {

        private val rxMethods = ArrayList<RxMethod<out T>>()

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            return this
        }

        private fun addLifecycleToMethods() {
            rxMethods.forEach {
                it.lifecycle = lifecycle
            }
        }

        fun build(): RxModule<T> {
            addLifecycleToMethods()
            return RxModule(RxMethodsExecutor(rxMethods, lifecycle))
        }
    }
}