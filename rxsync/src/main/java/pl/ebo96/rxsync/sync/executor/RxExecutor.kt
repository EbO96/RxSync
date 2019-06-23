package pl.ebo96.rxsync.sync.executor

import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.event.*
import java.util.concurrent.Executors

/**
 * This class is responsible for starting and cancelling execution of registered modules.
 *
 * @param rxModulesExecutor it is responsible for executing registered modules
 * @see RxModulesExecutor
 *
 * @param rxErrorListenerl user interface which provide information about execution failures
 */
class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val rxErrorListener: RxErrorListener) {

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
        compositeDisposable.add(rxModulesExecutor.execute(onUiThreadErrorHandler))
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

        private val rxModulesBuilders = ArrayList<ModuleBuilder<out T>>()

        private lateinit var rxErrorListener: RxErrorListener
        private var rxProgressListener: RxProgressListener? = null
        private var rxResultListener: RxResultListener<T>? = null

        private var rxMethodEventHandler: RxMethodEventHandler? = null

        fun register(rxModule: ModuleBuilder<out T>): Builder<T> {
            rxModulesBuilders.add(rxModule)
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

        fun setEventHandler(rxMethodEventHandler: RxMethodEventHandler?): Builder<T> {
            this.rxMethodEventHandler = rxMethodEventHandler
            return this
        }

        fun setResultListener(rxResultListener: RxResultListener<T>): Builder<T> {
            this.rxResultListener = rxResultListener
            return this
        }

        fun build(): RxExecutor<T> {
            val rxExecutorInfo = RxExecutorInfo()
            val rxExecutorStateStore = RxExecutorStateStore(rxProgressListener, rxExecutorInfo)

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

            val modulesExecutor = RxModulesExecutor(rxModules, rxResultListener, rxMethodEventHandler, rxExecutorStateStore)
            return RxExecutor(modulesExecutor, rxErrorListener)
        }
    }

    companion object {

        const val TAG = "rxexecutor"

        private val processors = (Runtime.getRuntime().availableProcessors() / 2) + 1
        val SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(processors))

        fun onUi(code: () -> Unit) {
            Completable.complete()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .doOnComplete { code() }
                    .subscribe()
        }
    }
}