package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class RxModule<T : Any> private constructor(private val rxMethodsExecutor: RxMethodsExecutor<T>) {

    fun prepareMethods(): Observable<out T> {
        return rxMethodsExecutor.prepare().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    class Builder<T : Any> {

        private val rxMethods = ArrayList<RxMethod<out T>>()
        private var lastMethod: RxMethod<out T>? = null

        fun register(rxMethod: RxMethod<out T>): Builder<T> {
            rxMethods.add(rxMethod)
            lastMethod = rxMethod
            return this
        }

        fun modifyBeforeEmission(resultConsumer: (T, RxMethod<out T>?) -> Unit): Builder<T> {
            lastMethod?.doSomethingWithResult {
                resultConsumer(it, lastMethod)
            }
            return this
        }

        fun build(): RxModule<T> {
            return RxModule(RxMethodsExecutor(rxMethods))
        }
    }
}