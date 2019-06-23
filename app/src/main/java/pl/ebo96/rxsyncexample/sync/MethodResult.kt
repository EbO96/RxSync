package pl.ebo96.rxsyncexample.sync

data class MethodResult<T : Any>(val methodInfo: MethodInfo, val result: T?)