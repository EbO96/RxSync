package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsyncexample.sync.RxProgress
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import pl.ebo96.rxsyncexample.sync.event.RxEvent
import pl.ebo96.rxsyncexample.sync.event.RxExecutorStateStore
import java.util.concurrent.Executors

/**
 * This class is responsible for starting and cancelling execution of registered modules.
 *
 * @param rxModulesExecutor it is responsible for executing registered modules
 * @see RxModulesExecutor
 *
 * @param errorHandler user interface which provide information about execution failures
 */
class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val errorHandler: Consumer<Throwable>) {

    private var started: Boolean = false

    private var compositeDisposable = CompositeDisposable()

    private val onUiThreadErrorHandler: Consumer<Throwable> = getErrorHandlerOnUiThread()

    init {
        RxJavaPlugins.setErrorHandler(onUiThreadErrorHandler)
    }

    /**
     * Cancel already running execution and start new one
     */
    fun start(): CompositeDisposable? {
        cancel()
        compositeDisposable.add(rxModulesExecutor.execute(onUiThreadErrorHandler))
        return compositeDisposable
    }

    /**
     * Cancel execution
     */
    fun cancel() {
        compositeDisposable.clear()
        started = false
    }

    /**
     * Returns consumer which are operates on UI thread.
     * This is middleware between RxJava 'onError Consumer' and user registered 'onError Consumer'
     * @see Consumer
     */
    private fun getErrorHandlerOnUiThread(): Consumer<Throwable> = Consumer { error ->
        Completable.complete()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete {
                    errorHandler.accept(error)
                }
                .subscribe()
    }

    interface RxEventHandler {
        fun onNewRxEvent(error: Throwable, rxEvent: Consumer<RxEvent>)
    }

    class Builder<T : Any> {

        private val rxModulesBuilders = ArrayList<ModuleBuilder<out T>>()

        private lateinit var errorHandler: Consumer<Throwable>
        private var progressHandler: Consumer<RxProgress>? = null
        private var rxEventHandler: RxEventHandler? = null

        fun register(rxModule: ModuleBuilder<out T>): Builder<T> {
            rxModulesBuilders.add(rxModule)
            return this
        }

        fun setErrorHandler(errorHandler: Consumer<Throwable>): Builder<T> {
            this.errorHandler = errorHandler
            return this
        }

        fun setProgressHandler(progressHandler: Consumer<RxProgress>): Builder<T> {
            this.progressHandler = progressHandler
            return this
        }

        fun setEventHandler(rxEventHandler: RxEventHandler): Builder<T> {
            this.rxEventHandler = rxEventHandler
            return this
        }

        fun build(): RxExecutor<T> {
            val rxExecutorInfo = RxExecutorInfo()
            val rxExecutorStateStore = RxExecutorStateStore(progressHandler, rxExecutorInfo)

            val rxModules = rxModulesBuilders
                    .asSequence()
                    .map {
                        it.createModuleAndGet(rxExecutorStateStore.generateModuleId())
                    }
                    .onEach {
                        rxExecutorInfo.saveModuleInfo(it)
                    }
                    .toList()

            rxModulesBuilders.clear()

            val modulesExecutor = RxModulesExecutor(rxModules, rxEventHandler, rxExecutorStateStore)
            return RxExecutor(modulesExecutor, errorHandler)
        }
    }

    companion object {

        const val TAG = "rxexecutor"

        private val processors = (Runtime.getRuntime().availableProcessors() / 2) + 1
        val SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(processors))
    }
}