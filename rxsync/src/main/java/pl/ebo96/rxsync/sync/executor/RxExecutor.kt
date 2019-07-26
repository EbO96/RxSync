package pl.ebo96.rxsync.sync.executor

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsync.sync.RxDevice
import pl.ebo96.rxsync.sync.builder.ModuleFactory
import pl.ebo96.rxsync.sync.event.*
import pl.ebo96.rxsync.sync.method.MethodResult
import java.util.concurrent.TimeUnit

/**
 * This class is responsible for starting and cancelling execution of registered modules.
 *
 * @param rxModulesExecutor it is responsible for executing registered modules
 * @see RxModulesExecutor
 *
 * @param rxErrorListener user interface which provide information about execution failures
 */
class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val rxErrorListener: RxErrorListener,
        private val rxElapsedTimeListener: RxElapsedTimeListener?,
        private val chronometer: Observable<Long>?) {

    private var compositeDisposable = CompositeDisposable()
    private val onUiThreadErrorHandler: Consumer<Throwable> = getErrorHandlerOnUiThread()


    init {
        RxJavaPlugins.setErrorHandler(onUiThreadErrorHandler)
    }

    /**
     * Cancel already running execution and start new one
     */
    fun start() {
        cancel()
        compositeDisposable.add(rxModulesExecutor.execute(onUiThreadErrorHandler, chronometer, rxElapsedTimeListener))
    }

    /**
     * Cancel execution
     */
    fun cancel() {
        compositeDisposable.clear()
    }

    /**
     * Returns consumer which are operates on UI thread.
     * This is middleware between RxJava 'onError Consumer' and user registered RxErrorListener
     * @see RxErrorListener
     */
    private fun getErrorHandlerOnUiThread(): Consumer<Throwable> = Consumer { error ->
        onUi {
            rxErrorListener.onError(error)
        }
    }

    class Builder<T : Any> {

        private val rxModulesBuilders = ArrayList<ModuleFactory<out T>>()
        private lateinit var rxErrorListener: RxErrorListener
        private var rxProgressListener: RxProgressListener? = null
        private var rxElapsedTimeListener: RxElapsedTimeListener? = null
        private var rxResultListener: RxResultListener<T>? = null
        private var rxMethodEventHandler: RxMethodEventHandler? = null
        private var maxThreads = RxDevice.defaultThreadsLimit
        private var chronometer: Observable<Long>? = null

        fun register(rxModule: ModuleFactory<out T>): Builder<T> {
            rxModulesBuilders.add(rxModule)
            return this
        }

        fun register(vararg rxModule: ModuleFactory<out T>): Builder<T> {
            rxModule.forEach {
                rxModulesBuilders.add(it)
            }
            return this
        }

        fun setErrorListener(rxErrorListener: RxErrorListener): Builder<T> {
            this.rxErrorListener = rxErrorListener
            return this
        }

        fun setProgressListener(rxProgressListener: RxProgressListener?): Builder<T> {
            this.rxProgressListener = rxProgressListener
            return this
        }

        fun setElapsedTimeListener(rxElapsedTimeListener: RxElapsedTimeListener): Builder<T> {
            this.rxElapsedTimeListener = rxElapsedTimeListener
            chronometer = Observable.interval(1, TimeUnit.SECONDS)
                    .takeUntil { false }
                    .subscribeOn(AndroidSchedulers.mainThread())
            return this
        }

        fun setEventHandler(rxMethodEventHandler: RxMethodEventHandler?): Builder<T> {
            this.rxMethodEventHandler = rxMethodEventHandler
            return this
        }

        fun setResultListener(rxResultListener: RxResultListener<T>): Builder<T> {
            this.rxResultListener = rxResultListener
            return this
        }

        /**
         * Set number of threads used by modules. If module doesn't specify
         * number of threads available for methods then this value will be used.
         * If specified number of threads is smaller or equal 0 then default value will be used.
         * Default value is specified by RxExecutor library and it depends on number of available processors
         * @see RxDevice.defaultThreadsLimit
         */
        fun setThreadsLimit(limit: Int): Builder<T> {
            if (limit > 0) {
                maxThreads = limit
            }
            return this
        }

        fun build(): RxExecutor<T> {
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

            val rxNonDeferredModules: Flowable<MethodResult<out T>> = Flowable
                    .fromCallable {
                        nonDeferredModulesBuilders.map { builder ->
                            val module = builder.createModuleAndGet(rxExecutorStateStore.generateModuleId(), maxThreads)
                            val moduleMethods = module.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
                            rxExecutorInfo.saveModuleInfo(module)
                            moduleMethods
                        }
                    }
                    .flatMap { modules ->
                        Flowable.concat(modules)
                    }
                    .subscribeOn(Schedulers.computation())

            val rxDeferredModules: Flowable<MethodResult<out T>> = Flowable
                    .fromCallable {
                        deferredModulesBuilders.map { builder ->
                            val module = builder.createModuleAndGet(rxExecutorStateStore.generateModuleId(), maxThreads)
                            val moduleMethods = module.prepareMethods(rxMethodEventHandler, rxExecutorStateStore)
                            rxExecutorInfo.saveModuleInfo(module)
                            moduleMethods
                        }
                    }
                    .flatMap { modules ->
                        Flowable.concat(modules)
                    }
                    .subscribeOn(Schedulers.computation())

            val modulesExecutor = RxModulesExecutor(rxNonDeferredModules, rxDeferredModules, rxProgressListener, rxResultListener, rxExecutorStateStore)
            return RxExecutor(modulesExecutor, rxErrorListener, rxElapsedTimeListener, chronometer)
        }
    }

    companion object {

        const val TAG = "rxexecutor"

        fun onUi(code: () -> Unit) {
            Completable.complete()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .doOnComplete { code() }
                    .subscribe()
        }
    }
}