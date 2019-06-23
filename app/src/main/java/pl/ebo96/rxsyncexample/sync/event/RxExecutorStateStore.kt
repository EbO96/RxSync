package pl.ebo96.rxsyncexample.sync.event

import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import pl.ebo96.rxsyncexample.sync.MethodResult
import pl.ebo96.rxsyncexample.sync.RxProgress
import pl.ebo96.rxsyncexample.sync.executor.RxExecutorInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RxExecutorStateStore(private val progressHandler: Consumer<RxProgress>?, private val rxExecutorInfo: RxExecutorInfo) {

    private val doneMethods = ConcurrentHashMap<Int, Int>()

    private val methodsIdStore: AtomicInteger = AtomicInteger()

    private val moduleIdStore: AtomicInteger = AtomicInteger()

    fun generateModuleId(): Int {
        return moduleIdStore.getAndIncrement()
    }

    fun generateMethodId(): Int {
        return methodsIdStore.incrementAndGet()
    }

    private fun getDoneMethodsCount(): Int {
        return doneMethods.size
    }

    private fun getAllMethodsCount(): Int {
        return rxExecutorInfo.getMethodsCount()
    }

    fun <T : Any> updateProgress(): Consumer<MethodResult<out T>> = Consumer { methodResult ->
        val methodId = methodResult.methodInfo.getMethodId()
        doneMethods[methodId] = methodId

        val rxProgress = RxProgress(
                done = getDoneMethodsCount(),
                total = getAllMethodsCount(),
                result = methodResult.result
        )

        progressHandler?.accept(rxProgress)
    }

    fun reset(): Consumer<Disposable> = Consumer {
        doneMethods.clear()
    }
}