package pl.ebo96.rxsync.sync.event

interface RxResultListener<T : Any> {

    fun onNextResult(data: T?)

    fun onNextUiResult(data: T?)
}