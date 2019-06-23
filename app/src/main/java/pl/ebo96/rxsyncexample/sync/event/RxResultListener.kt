package pl.ebo96.rxsyncexample.sync.event

interface RxResultListener<T : Any> {

    fun onResult(data: T?)

    fun onUiResult(data: T?)
}