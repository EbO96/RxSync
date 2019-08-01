package pl.ebo96.rxsync.sync.event

/**
 * Callback used to observe errors which are not handled by user.
 * You can register this callback in [pl.ebo96.rxsync.sync.executor.RxExecutor.Builder].
 */
interface RxErrorListener {

    /**
     * Observe errors which are not handled by user.
     * @see error error thrown during execution
     */
    fun onError(error: Throwable)
}