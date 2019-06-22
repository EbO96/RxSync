package pl.ebo96.rxsyncexample.sync.event

import android.util.Log
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RxExecutorStateStore {

    private val doneMethods = ConcurrentHashMap<Int, Int>()

    private val numberOfMethods = ConcurrentHashMap<Int, Int>()

    private val methodsIdStore: AtomicInteger = AtomicInteger()

    fun generateMethodId(): Int {
        return methodsIdStore.incrementAndGet()
    }

    fun getDoneMethodsCount(): Int {
        return doneMethods.size
    }

    fun getAllMethodsCount(): Int {
        return numberOfMethods.size
    }

    fun <T : Any> storeMethodAsDoneWhenCompleted(rxMethod: RxMethod<out T>): Consumer<T> = Consumer {
        Log.d(RxExecutor.TAG, "storeMethodAsDoneWhenCompleted -> $it")
        doneMethods[rxMethod.id] = rxMethod.id
    }

    fun reset(): Consumer<Disposable> = Consumer {
        doneMethods.clear()
    }
}