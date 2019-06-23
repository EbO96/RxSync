package pl.ebo96.rxsyncexample.sync.event

interface RxMethodResultListener<T : Any> {

    fun onResult(data: T?)

    fun onUiResult(data: T?)
}