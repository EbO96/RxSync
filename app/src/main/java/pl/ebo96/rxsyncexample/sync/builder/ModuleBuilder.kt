package pl.ebo96.rxsyncexample.sync.builder

import pl.ebo96.rxsyncexample.sync.RxModule

abstract class ModuleBuilder<T : Any> {

    private var module: RxModule<out T>? = null

    fun createModuleAndGet(id: Int): RxModule<out T> = module
            ?: this.build(RxModule.Builder(id)).also { module = it }

    abstract fun build(builder: RxModule.Builder<T>): RxModule<out T>
}