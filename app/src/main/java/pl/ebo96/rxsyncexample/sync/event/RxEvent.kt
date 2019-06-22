package pl.ebo96.rxsyncexample.sync.event

sealed class RxEvent {
    object CANCEL : RxEvent()
    object NEXT : RxEvent()
    object RETRY : RxEvent()
}