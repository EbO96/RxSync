package pl.ebo96.rxsyncexample.sync

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.util.*

class RxModulesExecutor<T : Any>(rxModules: ArrayList<RxModule<T>>) {

    private var compositeDisposable = CompositeDisposable()
    private var operation: Observable<Any>

    init {
        val preparedMethods = rxModules.map {
            it.prepareMethods()
        }
        operation = Observable.concat(preparedMethods)
    }

    fun execute(progressHandler: Consumer<RxProgress>?) {
        val disposable = operation
                .subscribe {
                    progressHandler?.accept(RxProgress(0, 0, false, it))
                }

        compositeDisposable.add(disposable)
    }

    fun abort() {
        compositeDisposable.clear() //TODO or dispose
    }
}