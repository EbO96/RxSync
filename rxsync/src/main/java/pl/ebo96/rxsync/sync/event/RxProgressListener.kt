package pl.ebo96.rxsync.sync.event

import pl.ebo96.rxsync.sync.event.RxProgress

interface RxProgressListener {

    fun onProgress(rxProgress: RxProgress)
}