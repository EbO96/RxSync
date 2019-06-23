package pl.ebo96.rxsync.sync.event

interface RxResultListener<T : Any> {

    fun onResult(data: T?)

    fun onUiResult(data: T?)
}