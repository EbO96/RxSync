package pl.ebo96.rxsync.sync.event

interface RxErrorListener {

    fun onError(error: Throwable)
}