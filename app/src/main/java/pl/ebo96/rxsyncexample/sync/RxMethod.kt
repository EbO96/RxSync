package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class RxMethod<T> private constructor(val async: Boolean) {

    private lateinit var operation: Observable<out T>

    var tag = ""

    fun registerOperation(operation: Observable<out T>): RxMethod<T> {
        this.operation = if (async) {
            operation.subscribeOn(Schedulers.io())
        } else {
            operation
        }.observeOn(AndroidSchedulers.mainThread())

        return this
    }

    fun modifyResult(map: (T) -> T): RxMethod<T> {
        operation = operation.flatMap {
            Observable.just(map(it))
        }
        return this
    }

    fun doSomethingWithResult(modify: (T) -> Unit): RxMethod<T> {
        operation = operation.flatMap {
            modify(it)
            Observable.just(it)
        }
        return this
    }

    fun start(): Observable<out T> {
        return operation
    }

    companion object {

        fun <T> create(async: Boolean): RxMethod<T> {
            val rxMethod = RxMethod<T>(async)
            return rxMethod
        }
    }
}