package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

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
        return RxMethod.create<T>(async).registerOperation {
            Thread.sleep(delay)
            if (simulateError) {
                throw Exception("Simulated error for $returnObject")
            }
            returnObject
        }
    }
}