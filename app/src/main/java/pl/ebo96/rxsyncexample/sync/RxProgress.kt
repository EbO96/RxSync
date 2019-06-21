package pl.ebo96.rxsyncexample.sync

data class RxProgress constructor(val done: Int, val total: Int, val result: Any? = null) {

    val percentage: Float
        get() = (done / (total.toFloat().takeIf { it > 0f } ?: 1f)) * 100f
}