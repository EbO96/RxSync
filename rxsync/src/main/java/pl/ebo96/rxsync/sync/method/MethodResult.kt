package pl.ebo96.rxsync.sync.method

data class MethodResult<T : Any>(val methodInfo: MethodInfo, val result: T?)