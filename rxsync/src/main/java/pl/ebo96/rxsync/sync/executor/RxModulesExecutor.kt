package pl.ebo96.rxsync.sync.executor

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.module.RxModule
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxResultListener
import pl.ebo96.rxsync.sync.method.MethodResult

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxResultListener: RxResultListener<T>?,
                                             private val rxMethodEventHandler: RxMethodEventHandler?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>): Disposable {
        val modulesMethodsAsObservable = rxModules.map {
            it.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
        }

        return Observable.concat(modulesMethodsAsObservable)
                .listenForResults()
                .subscribeOn(RxExecutor.SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxResultListener), errorHandler)
    }

    private fun Observable<MethodResult<out T>>.listenForResults(): Observable<MethodResult<out T>> = this.compose {
        it.flatMap { methodResult ->
            rxResultListener?.onResult(methodResult.result)
            Observable.just(methodResult)
        }
    }
}