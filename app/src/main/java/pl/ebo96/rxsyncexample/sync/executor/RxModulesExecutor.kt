package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxEventHandler: RxExecutor.RxEventHandler?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>): Disposable {
        val modulesMethodsAsObservable = rxModules.map {
            it.prepareMethods(rxEventHandler, rxExecutorStateStore)
        }

        return Observable.concat(modulesMethodsAsObservable)
                .subscribeOn(RxExecutor.SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .subscribe(rxExecutorStateStore.updateProgress(), errorHandler)
    }
}