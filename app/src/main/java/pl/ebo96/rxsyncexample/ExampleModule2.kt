package pl.ebo96.rxsyncexample

import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder

class ExampleModule2 : ModuleBuilder<String>() {

    override fun build(builder: RxModule.Builder<String>): RxModule<String> {
        return builder
                .register(buildMethod(false, "Hello", 300, false))
                .register(buildMethod(false, "There", 0, false))
                .register(buildMethod(false, "Or", 150, false))
                .register(buildMethod(false, "Hello", 0, false))
                .register(buildMethod(false, "World", 100, false))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation(Observable.create<T> {
            Thread.sleep(delay)
            if (!it.serialize().isDisposed) {
//                Log.d(RxExecutor.TAG, "Thread -> ${Thread.currentThread().name}")
                if (simulateError) {
                    throw Exception("Simulated error for $returnObject")
                }
                it.onNext(returnObject)
                it.onComplete()
            }
        })
    }

}