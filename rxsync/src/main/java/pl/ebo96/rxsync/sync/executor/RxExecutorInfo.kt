package pl.ebo96.rxsync.sync.executor

import pl.ebo96.rxsync.sync.event.PreparedModules
import pl.ebo96.rxsync.sync.event.RxExecutorStateStore
import pl.ebo96.rxsync.sync.event.RxProgress
import pl.ebo96.rxsync.sync.module.ModuleInfo
import pl.ebo96.rxsync.sync.module.RxModule
import java.util.concurrent.ConcurrentSkipListSet

class RxExecutorInfo {

    private val modulesInfo = ConcurrentSkipListSet<ModuleInfo<out Any>>()

    fun saveModuleInfo(moduleInfo: ModuleInfo<out Any>) {
        this.modulesInfo.add(moduleInfo)
    }

    fun getMethodsCount(): Int {
        return modulesInfo.sumBy { it.getMethodsCount() }
    }

    @Suppress("UNCHECKED_CAST")
    fun getRegisteredModules(rxExecutorStateStore: RxExecutorStateStore): PreparedModules {
        return modulesInfo
                .asSequence()
                .mapNotNull { it as? RxModule<out Any> }
                .map {
                    val moduleProgress = rxExecutorStateStore.getModuleProgress(it.getModuleId())
                    it to RxProgress(moduleProgress.done, it.getMethodsCount())
                }
                .toMap()
    }

    fun removeMethods() {
        return modulesInfo.forEach {
            it.removeModuleMethods()
        }
    }
}