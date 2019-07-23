package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .asyncMethodsAttemptsDelay(500)
                .asyncMethodsRetryAttempts(2)
                .register(RxMethod.create<Int>(false).registerOperation { 1 })
                .register(RxMethod.create<Int>(false).registerOperation { 2 })
                .register(RxMethod.create<Int>(false).registerOperation { 3 })
                .register(RxMethod.create<Int>(false).registerOperation { 4 })
                .register(RxMethod.create<Int>(false).registerOperation { 5 })
                .build()
    }

    override fun tag(): Any {
        return "ExampleModule"
    }
}