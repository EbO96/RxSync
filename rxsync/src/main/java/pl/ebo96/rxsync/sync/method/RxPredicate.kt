package pl.ebo96.rxsync.sync.method

import io.reactivex.Flowable

class RxPredicate<T>(private val value: Flowable<T>, private val predicate: Boolean) {

    fun getValue(): Flowable<T> {
        return if (predicate) {
            value
        } else {
            Flowable.empty()
        }
    }
}