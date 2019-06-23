package pl.ebo96.rxsyncexample.sync

abstract class RxDatabaseOperation<T> {

    abstract fun insert(data: T)

    abstract fun delete()
}