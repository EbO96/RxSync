package pl.ebo96.rxsyncexample.sync.event

data class RxProgress constructor(val done: Int, val total: Int) {

    val percentage: Float
        get() = (done / (total.toFloat().takeIf { it > 0f } ?: 1f)) * 100f
}