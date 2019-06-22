package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxEventHandler: RxExecutor.RxEventHandler?,
                                             val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(): Observable<T> {
        val modulesMethodsAsObservable = rxModules.map {
            Observable.just(it)
        }

        return Observable.concat(modulesMethodsAsObservable)
                .concatMapEager { module ->
                    module.prepareMethods(rxEventHandler, rxExecutorStateStore)
                }
                .subscribeOn(RxExecutor.SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
    }
}