package pl.ebo96.rxsyncexample

import io.reactivex.Observable
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.builder.ModuleBuilder

class ExampleModule2 : ModuleBuilder<String>(null) {

    override fun build(builder: RxModule.Builder<String>): RxModule<String> {
        return builder.register(
                RxMethod.create<String>(true).registerOperation(Observable.just("Hello From ${this.javaClass.simpleName}"))
        ).build()
    }

}