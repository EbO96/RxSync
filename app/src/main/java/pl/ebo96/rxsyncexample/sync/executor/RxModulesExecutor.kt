package pl.ebo96.rxsyncexample.sync.executor

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.RxProgress
import java.util.*

class RxModulesExecutor<T : Any> constructor(private val rxModules: ArrayList<RxModule<out T>>) {

    private var compositeDisposable = CompositeDisposable()

    fun execute(progressHandler: Consumer<RxProgress>?, errorHandler: Consumer<Throwable>) {
        val modulesMethodsAsObservable = rxModules.map {
            it.prepareMethods()
        }

        val disposable = Observable.concat(modulesMethodsAsObservable)
                .doOnEach {
                    if (!it.isOnError) {
                        progressHandler?.accept(
                                RxProgress(
                                        RxExecutor.Helper.doneMethods.size,
                                        RxExecutor.Helper.numberOfMethods.get(),
                                        it.value
                                )
                        )
                    }
                }
                .subscribe(Consumer {
                    /* DO NOTHING */
                }, errorHandler)

        compositeDisposable.add(disposable)
    }

    fun abort() {
//        compositeDisposable.dispose() //TODO or dispose
        compositeDisposable.clear() //TODO or dispose
    }
}