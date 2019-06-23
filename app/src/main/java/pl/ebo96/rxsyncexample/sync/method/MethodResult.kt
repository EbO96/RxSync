package pl.ebo96.rxsyncexample.sync.method

data class MethodResult<T : Any>(val methodInfo: MethodInfo, val result: T?)