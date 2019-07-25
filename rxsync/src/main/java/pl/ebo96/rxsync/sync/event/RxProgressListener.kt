package pl.ebo96.rxsync.sync.event

interface RxProgressListener {

    fun onProgress(rxProgress: RxProgress)

    //TODO return all and done methods count
    fun completed()
}