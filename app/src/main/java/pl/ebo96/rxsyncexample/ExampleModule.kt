package pl.ebo96.rxsyncexample

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class ExampleModule(lifecycle: RxExecutor.Lifecycle) : ModuleBuilder<Any>(lifecycle) {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .scheduler(Schedulers.computation())
                .also { builder1 ->
                    (1..100).forEach { index ->
                        builder1.register(buildMethod(false, index))
                    }
                }
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation(Observable.create {
            if (returnObject == 50 || returnObject == 76) {
                throw Throwable("This is error ${System.currentTimeMillis()}")
            }
            it.onNext(returnObject)
            it.onComplete()
        })
    }
}