package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.RxProgress
import java.util.*

class RxModulesExecutor<T : Any> constructor(rxModules: ArrayList<RxModule<out T>>) {


    private var compositeDisposable = CompositeDisposable()
    private var operation: Observable<T>

    init {
        val preparedMethods = rxModules.map {
            it.prepareMethods()
        }
        operation = Observable.concat(preparedMethods) //TODO RESTART all modules
    }

    fun execute(progressHandler: Consumer<RxProgress>?, errorHandler: Consumer<Throwable>) {
        val disposable = operation
                .doOnEach {
                    if (!it.isOnError) {
                        progressHandler?.accept(
                                RxProgress(
                                        RxExecutor.Helper.done,
                                        RxExecutor.Helper.numberOfMethods,
                                        false,
                                        it.value
                                )
                        )
                        RxExecutor.Helper.done++
                    }
                    if (it.isOnComplete || it.isOnError) {
                        RxExecutor.Helper.syncComplete()
                    }
                }
                .subscribe(Consumer { }, errorHandler)

        compositeDisposable.add(disposable)
    }

    fun abort() {
        compositeDisposable.clear() //TODO or dispose
    }
}