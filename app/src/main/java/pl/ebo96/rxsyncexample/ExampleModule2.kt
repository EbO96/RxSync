package pl.ebo96.rxsyncexample

import io.reactivex.Flowable
import pl.ebo96.rxsync.sync.builder.ModuleFactory
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule2 : ModuleFactory<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<out Any> {
        return builder
                .asyncMethodsAttemptsDelay(500)
                .asyncMethodsRetryAttempts(2)
                .register(RxMethod.create<String>(false).registerOperation(Flowable.just("Hello")))
                .register(RxMethod.create<String>(false).registerOperation(Flowable.just("World")))
                .register(RxMethod.create<String>(false).registerOperation(Flowable.just("!")))
                .build()

    }

    override fun tag(): Any {
        return "Example deferred module"
    }

    override fun isDeferred(): Boolean {
        return true
    }

}