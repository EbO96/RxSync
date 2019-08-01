package pl.ebo96.rxsync.sync.builder

import androidx.annotation.MainThread
import pl.ebo96.rxsync.sync.module.RxModule

/**
 * This class is responsible for creating modules
 * @see RxModule
 */
abstract class ModuleFactory<T : Any> {

    /**
     * Configured module
     */
    private var module: RxModule<out T>? = null

    /**
     * Build RxModule
     * @see RxModule
     */
    internal fun createModuleAndGet(id: Int, maxThreads: Int): RxModule<out T> {
        return this.build(RxModule.Builder(id, maxThreads, isDeferred(), this)).also {
            this.module = it
        }
    }

    /**
     * In this method user can configure module behavior and register methods.
     * @see RxModule
     * @see pl.ebo96.rxsync.sync.method.RxMethod
     */
    @MainThread
    abstract fun build(builder: RxModule.Builder<T>): RxModule<out T>

    /**
     * Custom 'tag' which user can use to identify module
     */
    open fun tag(): Any {
        return javaClass.simpleName
    }

    /**
     * User can mark module as 'deferred' which means that module will be executed in separated group
     * after all modules marked as 'non deferred'.
     */
    open fun isDeferred(): Boolean {
        return false
    }
}