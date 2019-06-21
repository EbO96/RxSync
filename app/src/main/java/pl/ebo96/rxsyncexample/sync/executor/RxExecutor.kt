package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
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

    init {
        RxJavaPlugins.setErrorHandler(onError())
    }

    fun start() {
        rxModulesExecutor.execute(onProgress(), onError())
    }

    fun stop() {
        rxModulesExecutor.abort()
    }

    private fun onError(): Consumer<Throwable> = Consumer { error ->
        errorHandler.also { consumer ->
            onUi {
                consumer.accept(error)
            }
        }
    }

    private fun onProgress(): Consumer<RxProgress> = Consumer { progress ->
        progressHandler?.also { consumer ->
            onUi {
                consumer.accept(progress)
            }
        }
    }

    interface Lifecycle {
        fun cannotRetry(error: Throwable, decision: Consumer<RxMethod.Event>)
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

    object Helper {

        val doneMethods = ConcurrentHashMap<Int, Int>()

        val numberOfMethods: AtomicInteger = AtomicInteger()
    }

    companion object {

        const val TAG = "rxexecutor"

        private val processors = (Runtime.getRuntime().availableProcessors() / 2) + 1
        val SCHEDULER = Schedulers.from(Executors.newFixedThreadPool(processors))
    }

    private fun onUi(processCode: () -> Unit) {
        Observable.empty<Unit>()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete { processCode() }
                .subscribe()
    }
}