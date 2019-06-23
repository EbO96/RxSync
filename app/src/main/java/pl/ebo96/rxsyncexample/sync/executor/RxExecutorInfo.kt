package pl.ebo96.rxsyncexample.sync.executor

import pl.ebo96.rxsyncexample.sync.ModuleInfo
import java.util.concurrent.ConcurrentSkipListSet

class RxExecutorInfo {

    private val modulesInfo = ConcurrentSkipListSet<ModuleInfo>()

    fun saveModuleInfo(moduleInfo: ModuleInfo) {
        this.modulesInfo.add(moduleInfo)
    }

    fun getMethodsCount(): Int {
        return modulesInfo.sumBy { it.getMethodsCount() }
    }
}