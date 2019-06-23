package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore
import pl.ebo96.rxsyncexample.sync.event.RxMethodResultListener
import pl.ebo96.rxsyncexample.sync.method.MethodResult
import pl.ebo96.rxsyncexample.sync.module.RxModule

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxMethodResultListener: RxMethodResultListener<T>?,
                                             private val rxEventHandler: RxExecutor.RxEventHandler?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>): Disposable {
        val modulesMethodsAsObservable = rxModules.map {
            it.prepareMethods(rxEventHandler, rxExecutorStateStore)
        }

        return Observable.concat(modulesMethodsAsObservable)
                .listenForResults()
                .subscribeOn(RxExecutor.SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxMethodResultListener), errorHandler)
    }

    private fun Observable<MethodResult<out T>>.listenForResults(): Observable<MethodResult<out T>> = this.compose {
        it.flatMap { methodResult ->
            rxMethodResultListener?.onResult(methodResult.result)
            Observable.just(methodResult)
        }
    }
}