package pl.ebo96.rxsync.sync.executor

import androidx.annotation.CallSuper
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

    private var elapsedTimeCounter: ElapsedTimeCounter? = null

    fun execute(errorHandler: Consumer<Throwable>, timeout: Long, rxElapsedTimeListener: RxElapsedTimeListener?): CompositeDisposable {
        val compositeDisposable = CompositeDisposable()
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

        elapsedTimeCounter = rxElapsedTimeListener?.let { ElapsedTimeCounter(it) }
        elapsedTimeCounter?.addToDisposable(compositeDisposable)
        elapsedTimeCounter?.start()

        val modules: Disposable = Flowable.concat(rxNonDeferredModules, rxDeferredModules)
                .applyTimeout(timeout)
                .listenForResults()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .doOnTerminate {
                    elapsedTimeCounter?.stop()
                    rxProgressListener?.completed(rxExecutorStateStore.getSummary())
                }
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResult(rxResultListener), errorHandler)

        return compositeDisposable.also {
            it.addAll(modules)
        }
    }

    private fun ArrayList<ModuleFactory<out T>>.prepareModuleMethods(rxExecutorStateStore: RxExecutorStateStore,
                                                                     rxExecutorInfo: RxExecutorInfo): Flowable<MethodResult<out T>> {
        return Flowable
                .fromCallable {
                    val moduleMethods = this@prepareModuleMethods.map { builder ->

                        val rxMethodEventHandlerDelegate = object : RxMethodEventHandler {
                            @CallSuper
                            override fun onNewRxEvent(error: Throwable, rxMethodEventConsumer: RxMethodEventConsumer) {
                                //Delegate
                                val rxMethodEventDelegate = object : RxMethodEventConsumer(rxMethodEventConsumer.consumer) {
                                    override fun onResponse(rxMethodEvent: RxMethodEvent) {
                                        super.onResponse(rxMethodEvent)
                                        elapsedTimeCounter?.restart()//Restart elapsed time counter
                                    }
                                }
                                //Delegate to user
                                rxMethodEventHandler?.onNewRxEvent(error, rxMethodEventDelegate)
                                elapsedTimeCounter?.stop()//Stop elapsed time counter
                            }
                        }

                        val module = builder.createModuleAndGet(rxExecutorStateStore.generateModuleId(), maxThreads)
                        val moduleMethods = module.prepareMethods(rxMethodEventHandlerDelegate, rxExecutorStateStore)
                        rxExecutorInfo.saveModuleInfo(module)
                        moduleMethods
                    }
                    rxProgressListener?.onModulesRegistered(rxExecutorInfo.getRegisteredModules(rxExecutorStateStore))
                    moduleMethods
                }
                .flatMap { moduleMethods ->
                    Flowable.concat(moduleMethods)
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