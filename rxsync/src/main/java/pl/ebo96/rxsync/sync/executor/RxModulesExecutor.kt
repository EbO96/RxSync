package pl.ebo96.rxsync.sync.executor

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsync.sync.event.RxElapsedTimeListener
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxProgressListener
import pl.ebo96.rxsync.sync.event.RxResultListener
import pl.ebo96.rxsync.sync.method.MethodResult
import java.util.concurrent.TimeUnit

class RxModulesExecutor<T : Any> constructor(private val rxNonDeferredModules: Flowable<MethodResult<out T>>,
                                             private val rxDeferredModules: Flowable<MethodResult<out T>>,
                                             private val rxProgressListener: RxProgressListener?,
                                             private val rxResultListener: RxResultListener<T>?,
                                             private val rxExecutorStateStore: RxExecutorStateStore) {

    fun execute(errorHandler: Consumer<Throwable>, chronometer: Observable<Long>?, timeout: Long, rxElapsedTimeListener: RxElapsedTimeListener?): CompositeDisposable {

        val elapsedTime: Disposable? = chronometer
                ?.doOnNext { seconds ->
                    rxElapsedTimeListener?.elapsed(seconds)
                }
                ?.subscribe()

        val modules: Disposable = Flowable.concat(rxNonDeferredModules, rxDeferredModules)
                .applyTimeout(timeout)
                .listenForResults()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(rxExecutorStateStore.reset())
                .doOnTerminate {
                    elapsedTime?.dispose()
                    rxProgressListener?.completed(rxExecutorStateStore.getSummary())
                }
                .subscribe(rxExecutorStateStore.updateProgressAndExposeResultOnUi(rxResultListener), errorHandler)

        return CompositeDisposable(modules).also {
            if (elapsedTime != null) {
                it.add(elapsedTime)
            }
        }
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