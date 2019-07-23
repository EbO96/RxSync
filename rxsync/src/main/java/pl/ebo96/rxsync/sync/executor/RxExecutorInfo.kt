package pl.ebo96.rxsync.sync.executor

import pl.ebo96.rxsync.sync.module.ModuleInfo
import java.util.concurrent.ConcurrentSkipListSet

class RxExecutorInfo {

    private val modulesInfo = ConcurrentSkipListSet<ModuleInfo>()

    fun saveModuleInfo(moduleInfo: ModuleInfo) {
        this.modulesInfo.add(moduleInfo)
    }

    fun getMethodsCount(): Int {
        return modulesInfo.sumBy { it.getMethodsCount() }
    }

    fun removeMethods() {
        return modulesInfo.forEach {
            if (it.isDeferred()) {
                it.removeModuleMethods()
            }
        }
    }
}