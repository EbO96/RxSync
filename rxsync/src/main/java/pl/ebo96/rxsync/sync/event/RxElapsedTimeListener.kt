package pl.ebo96.rxsync.sync.event

/**
 * Callback used to observe elapsed time. This time is measured in [pl.ebo96.rxsync.sync.executor.RxModulesExecutor.execute] method.
 * You can register this callback in [pl.ebo96.rxsync.sync.executor.RxExecutor.Builder].
 */
interface RxElapsedTimeListener {

    /**
     * Return number of seconds since the last start.
     * @param seconds number of seconds since the last start.
     */
    fun elapsed(seconds: Long)
}