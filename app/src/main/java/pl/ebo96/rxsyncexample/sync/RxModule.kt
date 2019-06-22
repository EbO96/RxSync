package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.executor.RxMethodsExecutor

class RxModule<T : Any> private constructor(private val rxMethodsExecutor: RxMethodsExecutor<out T>) {

    fun prepareMethods(rxEventHandler: RxExecutor.RxEventHandler?, rxExecutorStateStore: RxExecutorStateStore): Observable<out T> {
        return rxMethodsExecutor.prepare(rxEventHandler, rxExecutorStateStore)
    }

    class Builder<T : Any> {

        private val rxMethods = ArrayList<RxMethod<out T>>()

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            return this
        }

        fun build(): RxModule<T> {
            return RxModule(RxMethodsExecutor(rxMethods))
        }
    }
}