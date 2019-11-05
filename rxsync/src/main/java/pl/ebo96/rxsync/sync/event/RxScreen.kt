package pl.ebo96.rxsync.sync.event

interface RxScreen<T> {

    fun getSubject(): T?
}