package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.executor.RxMethodsExecutor

class RxModule<T : Any> private constructor(private val rxMethodsExecutor: RxMethodsExecutor<out T>) {

    fun prepareMethods(): Observable<out T> {
        return rxMethodsExecutor.prepare()
    }

    class Builder<T : Any>(private val lifecycle: RxExecutor.Lifecycle?) {

        private val rxMethods = ArrayList<RxMethod<out T>>()
        private var scheduler = Schedulers.io()

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            return this
        }

        @Suppress("UNCHECKED_CAST")
        fun flatRegister(getMethod: (T) -> RxMethod<out T>): Builder<T> {
            rxMethods.last().join {
                val method = getMethod(it) as RxMethod<Nothing>
                method
            }
            return this
        }

        fun scheduler(scheduler: Scheduler): Builder<T> {
            this.scheduler = scheduler
            return this
        }

        private fun addLifecycleToMethods() {
            rxMethods.forEach {
                it.lifecycle = lifecycle
            }
        }

        fun build(): RxModule<T> {
            addLifecycleToMethods()
            return RxModule(RxMethodsExecutor(rxMethods, scheduler))
        }
    }
}