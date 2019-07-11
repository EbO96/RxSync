package pl.ebo96.rxsync.sync.executor

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

class RxScheduler private constructor() {

    companion object {
        fun create(numberOfThreads: Int): Scheduler {
            return Schedulers.from(Executors.newFixedThreadPool(numberOfThreads))
        }
    }
}