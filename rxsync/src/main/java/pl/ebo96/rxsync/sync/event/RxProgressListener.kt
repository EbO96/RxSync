package pl.ebo96.rxsync.sync.event

import pl.ebo96.rxsync.sync.module.RxModule

interface RxProgressListener {

    fun onProgress(module: RxModule<*>, rxProgress: RxProgress)

    //TODO return all and done methods count
    fun completed()
}