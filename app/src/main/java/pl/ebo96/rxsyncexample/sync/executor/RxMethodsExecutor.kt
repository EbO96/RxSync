package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import pl.ebo96.rxsyncexample.sync.RxMethod

class RxMethodsExecutor<T : Any>(private val methods: ArrayList<RxMethod<out T>>,
                                 private val scheduler: Scheduler) {

    fun prepare(): Observable<T> {
        val methodsGroups: Map<Boolean, List<RxMethod<out T>>> = methods.groupBy { it.async }

        val nonAsyncMethods: List<Observable<out T>> = methodsGroups[NON_ASYNC]
                ?.map { it.start() }
                ?: emptyList()

        val asyncMethods: List<Observable<out T>>? = methodsGroups[ASYNC]
                ?.map { it.start() }
                ?: emptyList()

        val async: Observable<T> = Observable.merge(asyncMethods)
        val nonAsync: Observable<T> = Observable.concat(nonAsyncMethods)

        return Observable.concat(nonAsync, async).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        private const val ASYNC = true
        private const val NON_ASYNC = false
    }
}