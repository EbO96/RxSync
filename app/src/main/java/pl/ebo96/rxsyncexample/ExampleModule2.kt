package pl.ebo96.rxsyncexample

import android.util.Log
import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import pl.ebo96.rxsyncexample.sync.method.RxMethod
import pl.ebo96.rxsyncexample.sync.module.RxModule

class ExampleModule2 : ModuleBuilder<String>() {

    override fun build(builder: RxModule.Builder<String>): RxModule<String> {
        return builder
                .register(buildMethod(false, "Hello", 100, false))
                .register(buildMethod(false, "There", 0, false))
                .register(buildMethod(false, "Or", 150, false))
                .register(buildMethod(false, "Hello", 0, false))
                .register(buildMethod(false, "World", 100, false))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation(Observable.create<T> {
            Log.d(RxExecutor.TAG, "${this@ExampleModule2.javaClass.name}, thread -> ${Thread.currentThread().name}")
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