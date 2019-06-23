package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

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
        return RxMethod.create<T>(async).registerOperation {
            Thread.sleep(delay)
            if (simulateError) {
                throw Exception("Simulated error for $returnObject")
            }
            returnObject
        }
    }

}