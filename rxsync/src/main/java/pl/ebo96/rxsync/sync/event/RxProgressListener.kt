package pl.ebo96.rxsync.sync.event

interface RxProgressListener {

    fun onProgress(rxProgress: RxProgress)

    fun completed()
}