package pl.ebo96.rxsync.sync.executor

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxMethodEventHandler
import pl.ebo96.rxsync.sync.event.RxProgressListener
import pl.ebo96.rxsync.sync.event.RxResultListener
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.module.RxModule

class RxModulesExecutor<T : Any> constructor(private val rxModules: List<RxModule<out T>>,
                                             private val rxModulesBuilders: List<ModuleBuilder<out T>>,
                                             private val maxThreads: Int,
                                             private val rxExecutorInfo: RxExecutorInfo,
                                             private val rxProgressListener: RxProgressListener?,
                                             private val rxResultListener: RxResultListener<T>?,
                                             private val rxMethodEventHandler: RxMethodEventHandler?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>): Disposable {

        val nonDeferredModulesMethods = Flowable.concat(rxModules.map {
            it.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
        })

        val deferredModulesMethods = Flowable
                .fromCallable {
                    rxModulesBuilders.map { builder ->
                        val module = builder.createModuleAndGet(rxExecutorStateStore.generateModuleId(), maxThreads)
                        val moduleMethods = module.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
                        rxExecutorInfo.saveModuleInfo(module)
                        moduleMethods
                    }
                }
                .flatMap { modules ->
                    Flowable.concat(modules)
                }

        return Flowable.concat(nonDeferredModulesMethods, deferredModulesMethods)
                .listenForResults()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .doOnComplete { rxProgressListener?.completed() }
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxResultListener), errorHandler)
    }

    private fun Flowable<MethodResult<out T>>.listenForResults(): Flowable<MethodResult<out T>> = this.compose {
        it.flatMap { methodResult ->
            rxResultListener?.onNextResult(methodResult.result)
            Flowable.just(methodResult)
        }
    }

    companion object {
        private const val NON_DEFERRED_MODULES = false
        private const val DEFERRED_MODULES = true
    }
}