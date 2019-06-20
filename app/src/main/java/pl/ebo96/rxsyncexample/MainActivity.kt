package pl.ebo96.rxsyncexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsyncexample.sync.RxExecutor
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.RxModule

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val operation11 = Observable.create<String> { emitter ->
            var result = 0
            (0..300_000_000).forEach {
                if (it == 1679) {
                    //  emitter.onNext("Reached $it")
                }
                result += it
            }
            emitter.onNext("$result")
            emitter.onComplete()
        }

        val operation12 = Observable.create<String> { emitter ->
            var result = 0
            (0..100_500_000).forEach {
                if (it == 20000) {
                    // emitter.onNext("Reached $it")
                }
                result += it
            }
            emitter.onNext("$result")
            emitter.onComplete()
        }

        val operation13 = Observable.create<String> { emitter ->
            var result = 0
            (0..100_000).forEach {
                if (it == 100) {
                    // emitter.onNext("Reached $it")
                }
                result += it
            }
            emitter.onNext("$result")
            emitter.onComplete()
        }

        val method11 = RxMethod.create<String>(true)
                .registerOperation(operation11)
                .modifyResult {
                    "OPERATION 11 -> $it"
                }

        method11.tag = "method11"

        val method12 = RxMethod.create<String>(false)
                .registerOperation(operation12)
                .modifyResult {
                    "OPERATION 12(NON ASYNC) -> $it"
                }

        method12.tag = "method12"

        val method13 = RxMethod.create<String>(false)
                .registerOperation(operation13)
                .modifyResult {
                    "OPERATION 13 -> $it"
                }

        method13.tag = "method13"

        val exampleModule1: RxModule<Any> = RxModule.Builder<Any>()
                .register(method11)
                .modifyBeforeEmission { result, method ->
                    setResultOnTextView("for method11: $result and ${method?.tag}")
                }
                .register(method13)
                .register(method12)
                .modifyBeforeEmission { result, method ->
                    setResultOnTextView("for method12:  $result and ${method?.tag}")
                }
                .build()

        val rxExecutor: RxExecutor<Any> = RxExecutor.Builder<Any>()
                .register(exampleModule1)
                .setProgressHandler(Consumer {
                    setResultOnTextView("$it")
                })
                .setErrorHandler(Consumer {
                    setResultOnTextView("Error: ${it.message}")
                    it.printStackTrace()
                })
                .build()

        startButton.setOnClickListener {
            rxExecutor.start()
        }

        stopButton.setOnClickListener {
            clearResultTextView()
            rxExecutor.stop()
        }
    }

    private fun clearResultTextView() {
        resultTextView.text = ""
    }

    private fun setResultOnTextView(text: String) {
        resultTextView.text = "${resultTextView.text}\n${text}\n----------------------------"
    }
}
