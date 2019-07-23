package pl.ebo96.rxsyncexample

import pl.ebo96.rxsync.sync.builder.ModuleBuilder
import pl.ebo96.rxsync.sync.method.RxMethod
import pl.ebo96.rxsync.sync.module.RxModule

class ExampleModule2 : ModuleBuilder<Any>() {

    override fun build(builder: RxModule.Builder<Any>): RxModule<out Any> {
        RestApi.results.forEachIndexed { index, result ->
            builder.register(RxMethod.create<String>(false).registerOperation { "$result + [${index + 1}]" })
        }

        return builder
                .build()
    }

    override fun isDeferred(): Boolean {
        return true
    }
}