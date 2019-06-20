package pl.ebo96.rxsyncexample.sync

data class RxProgress(val done: Int, val total: Int, var stopped: Boolean, val result: Any? = null)