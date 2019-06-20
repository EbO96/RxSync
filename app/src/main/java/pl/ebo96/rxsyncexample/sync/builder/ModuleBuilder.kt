package pl.ebo96.rxsyncexample.sync.builder

import pl.ebo96.rxsyncexample.sync.RxModule
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

abstract class ModuleBuilder<T : Any>(lifecycle: RxExecutor.Lifecycle?) {

    val module = this.build(RxModule.Builder(lifecycle))

    abstract fun build(builder: RxModule.Builder<T>): RxModule<out T>
}