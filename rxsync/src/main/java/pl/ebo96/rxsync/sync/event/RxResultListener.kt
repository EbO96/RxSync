package pl.ebo96.rxsync.sync.event

import pl.ebo96.rxsync.sync.method.MethodResult

interface RxResultListener<T : Any> {

    fun onNextResult(data: MethodResult<out T>)

    fun onNextUiResult(data: MethodResult<out T>)
}