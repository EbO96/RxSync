package pl.ebo96.rxsyncexample

import android.util.Log
import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.method.RxMethod
import pl.ebo96.rxsyncexample.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .register(buildMethod(false, TestResult("Before modification")).doSomethingWithResult {
                    it?.testText = "After modification"
                })
                .register(buildMethod(true, 2, 500, false))
                .register(buildMethod(false, 3, 10, false))
                .register(buildMethod(false, 4, 200))
                .register(buildMethod(true, 5, 300, false))
                .register(buildMethod(false, 6, 400))
                .register(buildMethod(true, 7, 100, false))
                .register(buildMethod(false, 8, 10))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation(Observable.create<T> {
            Log.d(RxExecutor.TAG, "${this@ExampleModule.javaClass.name}, thread -> ${Thread.currentThread().name}, result -> $returnObject")
            Thread.sleep(delay)
            if (!it.serialize().isDisposed) {
                if (simulateError) {
                    throw Exception("Simulated error for $returnObject")
                }
                it.onNext(returnObject)
                it.onComplete()
            }
        })
    }
}