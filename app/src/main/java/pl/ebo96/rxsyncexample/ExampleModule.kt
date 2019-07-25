package pl.ebo96.rxsyncexample

import io.reactivex.Flowable
import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.method.RxPredicate
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .asyncMethodsAttemptsDelay(500)
                .asyncMethodsRetryAttempts(2)
                .register(RxMethod.create<Int>(true).conditionalRegister {
                    RxPredicate(Flowable.just(1), false)
                })
                .register(RxMethod.create<Int>(true).registerOperationDeferred { Flowable.just(2) })
                .register(RxMethod.create<Int>(true).registerOperationDeferred { Flowable.just(3) })
                .register(RxMethod.create<Int>(true).registerOperationDeferred { Flowable.just(4) })
                .register(RxMethod.create<Int>(true).registerOperationDeferred { Flowable.just(5) })
                .build()
    }

    override fun tag(): Any {
        return "ExampleModule"
    }
}