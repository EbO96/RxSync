package pl.ebo96.rxsync.sync.executor

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsync.sync.builder.ModuleFactory
import pl.ebo96.rxsync.sync.event.*
import pl.ebo96.rxsync.sync.method.MethodResult
import java.util.concurrent.TimeUnit

class RxModulesExecutor<T : Any> constructor(private val rxModulesBuilders: ArrayList<ModuleFactory<out T>>,
                                             private val rxProgressListener: RxProgressListener?,
                                             private val rxResultListener: RxResultListener<T>?,
                                             private val rxMethodEventHandler: RxMethodEventHandler?,
                                             private val maxThreads: Int) {

    fun execute(errorHandler: Consumer<Throwable>, chronometer: Observable<Long>?, timeout: Long, rxElapsedTimeListener: RxElapsedTimeListener?): CompositeDisposable {

        val rxExecutorInfo = RxExecutorInfo()
        val rxExecutorStateStore = RxExecutorStateStore(rxProgressListener, rxExecutorInfo)

        val deferredModulesBuilders = ArrayList<ModuleFactory<out T>>()
        val nonDeferredModulesBuilders = ArrayList<ModuleFactory<out T>>()

        rxModulesBuilders.forEach { moduleBuilder ->
            if (moduleBuilder.isDeferred()) {
                deferredModulesBuilders.add(moduleBuilder)
            } else {
                nonDeferredModulesBuilders.add(moduleBuilder)
            }
        }

        val rxNonDeferredModules: Flowable<MethodResult<out T>> = nonDeferredModulesBuilders.prepareModuleMethods(rxExecutorStateStore, rxExecutorInfo)
        val rxDeferredModules: Flowable<MethodResult<out T>> = deferredModulesBuilders.prepareModuleMethods(rxExecutorStateStore, rxExecutorInfo)

        val elapsedTime: Disposable? = chronometer
                ?.doOnNext { seconds ->
                    rxElapsedTimeListener?.elapsed(seconds)
                }
                ?.subscribe()

        val modules: Disposable = Flowable.concat(rxNonDeferredModules, rxDeferredModules)
                .applyTimeout(timeout)
                .listenForResults()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .doOnTerminate {
                    elapsedTime?.dispose()
                    rxProgressListener?.completed(rxExecutorStateStore.getSummary())
                }
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxResultListener), errorHandler)

        return CompositeDisposable(modules).also {
            if (elapsedTime != null) {
                it.add(elapsedTime)
            }
        }
    }

    private fun ArrayList<ModuleFactory<out T>>.prepareModuleMethods(rxExecutorStateStore: RxExecutorStateStore,
                                                                     rxExecutorInfo: RxExecutorInfo): Flowable<MethodResult<out T>> {
        return Flowable
                .fromCallable {
                    val moduleMethods = this@prepareModuleMethods.map { builder ->
                        val module = builder.createModuleAndGet(rxExecutorStateStore.generateModuleId(), maxThreads)
                        val moduleMethods = module.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
                        rxExecutorInfo.saveModuleInfo(module)
                        moduleMethods
                    }
                    rxProgressListener?.onModulesRegistered(rxExecutorInfo.getRegisteredModules(rxExecutorStateStore))
                    moduleMethods
                }
                .flatMap { modules ->
                    Flowable.concat(modules)
                }
                .subscribeOn(Schedulers.computation())
    }

    private fun Flowable<MethodResult<out T>>.applyTimeout(timeout: Long): Flowable<MethodResult<out T>> = this.compose {
        if (timeout > 0) {
            it.timeout(timeout, TimeUnit.MILLISECONDS)
        } else {
            it
        }
    }

    private fun Flowable<MethodResult<out T>>.listenForResults(): Flowable<MethodResult<out T>> = this.compose {
        it.flatMap { methodResult ->
            rxResultListener?.onNextResult(methodResult)
            Flowable.just(methodResult)
        }
    }

    companion object {
        private const val NON_DEFERRED_MODULES = false
        private const val DEFERRED_MODULES = true
    }
}