package pl.ebo96.rxsync.sync.executor

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.event.RxProgressListener
import pl.ebo96.rxsync.sync.event.RxResultListener
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.module.RxModule

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxProgressListener: RxProgressListener?,
                                             private val rxResultListener: RxResultListener<T>?,
                                             private val rxMethodEventHandler: RxMethodEventHandler?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>): Disposable {
        val modulesMethodsAsObservable = rxModules.map {
            it.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
        }

        return Flowable.concat(modulesMethodsAsObservable)
                .listenForResults()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .doOnComplete { rxProgressListener?.completed() }
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxResultListener), errorHandler)
    }

    private fun Flowable<MethodResult<out T>>.listenForResults(): Flowable<MethodResult<out T>> = this.compose {
        it.flatMap { methodResult ->
            rxResultListener?.onResult(methodResult.result)
            Flowable.just(methodResult)
        }
    }
}