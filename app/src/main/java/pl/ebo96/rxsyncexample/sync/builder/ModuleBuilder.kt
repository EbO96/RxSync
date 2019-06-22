package pl.ebo96.rxsyncexample.sync.builder

import pl.ebo96.rxsyncexample.sync.RxModule

abstract class ModuleBuilder<T : Any> {

    val module: RxModule<out T> by lazy { this.build(RxModule.Builder()) }

    abstract fun build(builder: RxModule.Builder<T>): RxModule<out T>
}