package pl.ebo96.rxsync.sync.module

interface ModuleInfo<T : Any> {

    fun isDeferred(): Boolean

    fun getModuleId(): Int

    fun getMethodsCount(): Int

    fun removeModuleMethods()
}