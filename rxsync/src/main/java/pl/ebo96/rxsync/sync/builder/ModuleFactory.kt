package pl.ebo96.rxsync.sync.builder

import androidx.annotation.MainThread
import pl.ebo96.rxsync.sync.module.RxModule

abstract class ModuleFactory<T : Any> {

    private var module: RxModule<out T>? = null

    fun createModuleAndGet(id: Int, maxThreads: Int): RxModule<out T> = module
            ?: this.build(RxModule.Builder(id, maxThreads, isDeferred(), this)).also { module = it }

    /**
     * By default operates on Main thread
     */
    @MainThread
    abstract fun build(builder: RxModule.Builder<T>): RxModule<out T>

    /**
     *
     */
    open fun tag(): Any {
        return javaClass.simpleName
    }

    /**
     * Deferred modules can contain dynamic number of methods.
     * For example you can shouldRegister methods in 'forEach' loop based on data from previously executed methods
     */
    open fun isDeferred(): Boolean {
        return false
    }
}