package pl.ebo96.rxsyncexample.sync

import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins

class RxExecutor<T : Any> private constructor(
        private val rxModulesExecutor: RxModulesExecutor<T>,
        private val errorHandler: Consumer<Throwable>,
        private val progressHandler: Consumer<RxProgress>?
) {

    private var started: Boolean = false

    fun start() {
        if (started) return
        RxJavaPlugins.setErrorHandler(errorHandler)
        rxModulesExecutor.execute(progressHandler)
        started = true
    }

    fun stop() {
        if (!started) return
        RxJavaPlugins.setErrorHandler(null)
        rxModulesExecutor.abort()
        started = false
    }

    class Builder<T : Any> {

        private val rxModules = ArrayList<RxModule<T>>()
        private lateinit var errorHandler: Consumer<Throwable>
        private var progressHandler: Consumer<RxProgress>? = null

        fun register(rxModule: RxModule<T>): Builder<T> {
            rxModules.add(rxModule)
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
}