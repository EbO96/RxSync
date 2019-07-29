package pl.ebo96.rxsync.sync.event

import pl.ebo96.rxsync.sync.module.RxModule

typealias PreparedModules = Map<RxModule<*>, RxProgress>

interface RxProgressListener {

    fun onProgress(rxProgress: RxProgress)
    fun onModuleProgress(module: RxModule<*>, rxProgress: RxProgress)
    fun onModulesRegistered(modules: PreparedModules)

    fun completed(rxProgress: RxProgress)
}