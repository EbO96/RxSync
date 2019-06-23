package pl.ebo96.rxsync.sync.event

sealed class RxMethodEvent {
    object CANCEL : RxMethodEvent()
    object NEXT : RxMethodEvent()
    object RETRY : RxMethodEvent()
}