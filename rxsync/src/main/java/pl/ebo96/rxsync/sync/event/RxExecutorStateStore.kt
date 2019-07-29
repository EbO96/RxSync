package pl.ebo96.rxsync.sync.event

import io.reactivex.functions.Consumer
import org.reactivestreams.Subscription
import pl.ebo96.rxsync.sync.executor.RxExecutorInfo
import pl.ebo96.rxsync.sync.method.MethodResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RxExecutorStateStore(private val rxProgressListener: RxProgressListener?, private val rxExecutorInfo: RxExecutorInfo) {

    private val doneMethods = ConcurrentHashMap<Int, Int>()

    /**
     * [module id] -> [done methods -> total methods]
     */
    private val perModuleProgress = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Int>>()

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

    fun getModuleProgress(id: Int): RxProgress {
        val moduleProgress = perModuleProgress[id]
        return RxProgress(
                done = moduleProgress?.keys?.size ?: 0,
                total = moduleProgress?.values?.firstOrNull() ?: 0
        )
    }

    fun getSummary(): RxProgress {
        return RxProgress(
                done = getDoneMethodsCount(),
                total = getAllMethodsCount()
        )
    }

    fun <T : Any> updateProgressAndExposeResultOnUi(rxResultListener: RxResultListener<T>?): Consumer<MethodResult<out T>> = Consumer { methodResult ->
        val methodId = methodResult.methodInfo.getMethodId()
        doneMethods[methodId] = methodId

        val moduleDoneMethods: ConcurrentHashMap<Int, Int> = perModuleProgress[methodResult.module.getModuleId()]
                ?: ConcurrentHashMap()

        moduleDoneMethods[methodId] = methodResult.module.getMethodsCount()
        perModuleProgress[methodResult.module.getModuleId()] = moduleDoneMethods

        val rxProgress = getSummary()

        rxResultListener?.onNextUiResult(methodResult)
        rxProgressListener?.onProgress(rxProgress)

        val moduleProgress = getModuleProgress(methodResult.module.getModuleId())

        rxProgressListener?.onModuleProgress(methodResult.module, moduleProgress)
    }

    fun reset(): Consumer<in Subscription> = Consumer {
        doneMethods.clear()
        perModuleProgress.clear()
        rxExecutorInfo.removeMethods()
    }
}