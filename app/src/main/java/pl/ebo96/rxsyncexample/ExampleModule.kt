package pl.ebo96.rxsyncexample

import io.reactivex.Flowable
import pl.ebo96.rxsync.sync.builder.ModuleFactory
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleFactory<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .asyncMethodsRetryAttempts(1)
                .register(RxMethod.create<Int>(true).registerOperation(Flowable.fromCallable {
                    Thread.sleep(8000)
                    throw Exception("Error :)")
                    1
                }))
                .register(RxMethod.create<Int>(true).registerOperation(Flowable.empty()))
                .register(RxMethod.create<Int>(true).registerOperation(Flowable.just(3)))
                .build()
    }

    override fun tag(): Any {
        return "ExampleModule"
    }
}