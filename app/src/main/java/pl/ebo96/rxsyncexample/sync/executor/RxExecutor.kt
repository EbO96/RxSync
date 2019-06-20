package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.RxProgress
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder

class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val errorHandler: Consumer<Throwable>,
        private val progressHandler: Consumer<RxProgress>?
) {

    init {
        RxJavaPlugins.setErrorHandler(errorHandler)
    }

    fun start() {
        rxModulesExecutor.execute(progressHandler, onError())
    }

    fun stop() {
        rxModulesExecutor.abort()
        Helper.syncComplete()
    }

    private fun onError(): Consumer<Throwable> = Consumer {
        errorHandler.accept(it)
    }

    fun shutdown() {
        Helper.clear()
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
        var done: Int = 0
            @Synchronized
            set
            @Synchronized
            get

        var numberOfMethods = 0
            @Synchronized
            set
            @Synchronized
            get

        fun clear() {
            done = 0
            numberOfMethods = 0
        }

        fun syncComplete() {
            done = 0
        }
    }
}