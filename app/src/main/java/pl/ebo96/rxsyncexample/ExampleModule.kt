package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .register(buildMethod(true, 2, 1000, false))
                .register(buildMethod(true, 3, 1000, false))
                .register(buildMethod(true, 4, 1000))
                .register(buildMethod(true, 5, 1000, false))
                .register(buildMethod(true, 6, 1000))
                .register(buildMethod(true, 7, 1000, false))
                .register(buildMethod(true, 8, 1000))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async).registerOperation {
            Thread.sleep(delay)
            if (simulateError) {
                throw Exception("Simulated error for $returnObject")
            }
            returnObject
        }
    }
}