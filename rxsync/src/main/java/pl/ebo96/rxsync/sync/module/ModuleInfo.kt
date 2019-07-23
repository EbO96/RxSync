package pl.ebo96.rxsync.sync.module

interface ModuleInfo {

    fun isDeferred(): Boolean

    fun getModuleId(): Int

    fun getMethodsCount(): Int

    fun removeModuleMethods()
}