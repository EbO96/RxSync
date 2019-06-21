package pl.ebo96.rxsyncexample.sync.executor

import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import pl.ebo96.rxsyncexample.sync.RxModule
import java.util.*

class RxModulesExecutor<T : Any> constructor(private val rxModules: ArrayList<RxModule<out T>>) {

    fun execute(): Observable<T> {
        val modulesMethodsAsObservable = rxModules.map {
            Observable.just(it)
        }

        return Observable.concat(modulesMethodsAsObservable)
                .concatMapEager { module ->
                    module.prepareMethods()
                }
                .subscribeOn(RxExecutor.SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    Log.d(RxExecutor.TAG, "SUBSCRIBED")
                }
                .doOnEach {
                    Log.d(RxExecutor.TAG, "onEach")
                    //TODO update total methods size
                }

    }
}