package pl.ebo96.rxsync.sync

object RxDevice {

    val availableProcessors = Runtime.getRuntime().availableProcessors()
    val defaultThreadsLimit = (availableProcessors / 2) + 1
}