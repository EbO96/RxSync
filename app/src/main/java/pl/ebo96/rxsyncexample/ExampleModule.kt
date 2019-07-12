package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<Any> {
        return builder
                .asyncMethodsAttemptsDelay(500)
                .asyncMethodsRetryAttempts(2)
                .register(buildMethod(true, 2, 300, false))
                .register(buildMethod(true, 3, 300, false))
                .register(buildMethod(true, 4, 300))
                .register(buildMethod(true, 5, 300, true))
                .register(buildMethod(true, 6, 300))
                .register(buildMethod(true, 7, 300, false))
                .register(buildMethod(true, 8, 300))
                .build()
    }

    private fun <T : Any> buildMethod(async: Boolean, returnObject: T, delay: Long = 0, simulateError: Boolean = false): RxMethod<T> {
        return RxMethod.create<T>(async, 2, 1000L).registerOperation {
            Thread.sleep(delay)
            if (simulateError) {
                throw Exception("Simulated error for $returnObject")
            }
            returnObject
        }
    }
}