package pl.ebo96.rxsync.sync.executor

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import pl.ebo96.rxsync.sync.RxDevice
import pl.ebo96.rxsync.sync.builder.ModuleFactory
import pl.ebo96.rxsync.sync.event.*
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
        private val timeout: Long) {

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
        compositeDisposable.add(rxModulesExecutor.execute(onUiThreadErrorHandler, timeout, rxElapsedTimeListener))
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
        private var timeout: Long = 0

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

        fun timeout(timeout: Long): Builder<T> {
            this.timeout = timeout
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

        fun setThreadsLimit(limit: Int): Builder<T> {
            if (limit > 0) {
                maxThreads = limit
            }
            return this
        }

        fun build(): RxExecutor<T> {
            val modulesExecutor = RxModulesExecutor(rxModulesBuilders, rxProgressListener, rxResultListener, rxMethodEventHandler, maxThreads)
            return RxExecutor(modulesExecutor, rxErrorListener, rxElapsedTimeListener, timeout)
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