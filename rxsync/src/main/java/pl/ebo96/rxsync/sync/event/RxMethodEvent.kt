package pl.ebo96.rxsync.sync.event

/**
 * Constants for user, to decide what to do when error occurred.
 */
sealed class RxMethodEvent {

    /**
     * Abort whole process
     */
    object CANCEL : RxMethodEvent()

    /**
     * Omit failed method and continue
     */
    object NEXT : RxMethodEvent()

    /**
     * Try to execute failed methods again
     */
    object RETRY : RxMethodEvent()
}