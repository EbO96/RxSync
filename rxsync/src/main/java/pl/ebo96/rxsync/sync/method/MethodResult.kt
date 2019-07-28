package pl.ebo96.rxsync.sync.method

import pl.ebo96.rxsync.sync.module.RxModule

data class MethodResult<T : Any>(val methodInfo: MethodInfo, val result: T?, val payload: Any?, val module: RxModule<*>)