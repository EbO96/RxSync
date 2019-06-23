package pl.ebo96.rxsyncexample

import android.util.Log
import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .register(buildMethod(false, 1))
                .register(buildMethod(false, 2, 500))
                .register(buildMethod(false, 3, 10, false))
                .register(buildMethod(false, 4, 200))
                .register(buildMethod(false, 5, 300, false))
                .register(buildMethod(false, 6, 400))
                .register(buildMethod(false, 7, 100, false))
                .register(buildMethod(false, 8, 10))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation(Observable.create<T> {
            Thread.sleep(delay)
            if (!it.serialize().isDisposed) {
                Log.d(RxExecutor.TAG, "Thread -> ${Thread.currentThread().name}")
                if (simulateError) {
                    throw Exception("Simulated error for $returnObject")
                }
                it.onNext(returnObject)
                it.onComplete()
            }
        })
    }
}