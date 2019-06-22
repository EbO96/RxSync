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
 * //TODO
 */
class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val errorHandler: Consumer<Throwable>,
        private val progressHandler: Consumer<RxProgress>?) {

    private var compositeDisposable = CompositeDisposable()

    private val onUiThreadErrorHandler: Consumer<Throwable> = getErrorHandlerOnUiThread()

    init {
        RxJavaPlugins.setErrorHandler(onUiThreadErrorHandler)
    }

    fun start() {
        val disposable = rxModulesExecutor.execute()
                .doOnEach {
                    //TODO check and update number of all methods and done methods
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer {
                    progressHandler?.accept(
                            RxProgress(
                                    rxModulesExecutor.rxExecutorStateStore.getDoneMethodsCount(),
                                    rxModulesExecutor.rxExecutorStateStore.getAllMethodsCount(),
                                    it
                            )
                    )
                }, onUiThreadErrorHandler)

        compositeDisposable.add(disposable)
    }

    fun cancel() {
        compositeDisposable.clear()
    }

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
            val rxModules = rxModulesBuilders.map { it.module }
            rxModulesBuilders.clear()
            return RxExecutor(RxModulesExecutor(rxModules, rxEventHandler, RxExecutorStateStore()), errorHandler, progressHandler)
        }
    }

    companion object {

        const val TAG = "rxexecutor"

        private val processors = (Runtime.getRuntime().availableProcessors() / 2) + 1
        val SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(processors))
    }
}