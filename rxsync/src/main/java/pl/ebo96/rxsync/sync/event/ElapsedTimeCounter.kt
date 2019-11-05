package pl.ebo96.rxsync.sync.event

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ElapsedTimeCounter(private val elapsedTimeListener: RxElapsedTimeListener) {

    private val compositeDisposable = CompositeDisposable()
    private var elapsed = 0L

    fun start() {
        stop()
        val disposable = Observable.interval(1, TimeUnit.SECONDS)
                .takeUntil { false }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    elapsed = it
                    elapsedTimeListener.elapsed(it)
                }
        compositeDisposable.add(disposable)
    }

    fun stop() {
        compositeDisposable.clear()
    }

    fun restart() {
        start()
    }

    fun addToDisposable(disposable: CompositeDisposable) {
        disposable.add(compositeDisposable)
    }
}