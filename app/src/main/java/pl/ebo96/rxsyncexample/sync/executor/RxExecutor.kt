package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.RxProgress
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * //TODO
 */
class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val errorHandler: Consumer<Throwable>,
        private val progressHandler: Consumer<RxProgress>?
) {

    private var compositeDisposable = CompositeDisposable()

    private val doneMethods = ConcurrentHashMap<Int, Int>()

    private val numberOfMethods: AtomicInteger = AtomicInteger()

    init {
        RxJavaPlugins.setErrorHandler(errorHandler)
    }

    fun start() {
        val disposable = rxModulesExecutor.execute()
                .doOnEach {
                    //TODO check and update number of all methods and done methods
                }
                .subscribe(Consumer {
                    progressHandler?.accept(
                            RxProgress(
                                    doneMethods.size,
                                    numberOfMethods.get(),
                                    it
                            )
                    )
                }, errorHandler)

        compositeDisposable.add(disposable)
    }

    fun stop() {
        compositeDisposable.clear()
    }

    interface RxEvent {
        fun onEvent(error: Throwable, event: Consumer<RxMethod.Event>)
    }

    class Builder<T : Any> {

        private val rxModules = ArrayList<RxModule<out T>>()
        private lateinit var errorHandler: Consumer<Throwable>
        private var progressHandler: Consumer<RxProgress>? = null

        fun register(rxModule: ModuleBuilder<out T>): Builder<T> {
            rxModules.add(rxModule.module)
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

        fun build(): RxExecutor<T> {
            return RxExecutor(RxModulesExecutor(rxModules), errorHandler, progressHandler)
        }
    }

    companion object {

        const val TAG = "rxexecutor"

        private val processors = (Runtime.getRuntime().availableProcessors() / 2) + 1
        val SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(processors))
    }
}