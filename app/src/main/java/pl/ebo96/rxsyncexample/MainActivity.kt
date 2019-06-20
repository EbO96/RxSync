package pl.ebo96.rxsyncexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class MainActivity : AppCompatActivity(), RxExecutor.Lifecycle {

    private lateinit var rxExecutor: RxExecutor<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxExecutor = RxExecutor.Builder<Any>()
                .register(ExampleModule(this))
                .setProgressHandler(Consumer {
                    progressBar.max = it.total
                    progressBar.progress = it.done
                    progressPercentTextView.text = "${it.percentage}%"
                    setResultOnTextView("$it")
                })
                .setErrorHandler(Consumer {
                    clearUi()
                    setResultOnTextView("${it.message}")
                })
                .build()

        startButton.setOnClickListener {
            rxExecutor.start()
        }

        stopButton.setOnClickListener {
            clearUi()
            rxExecutor.stop()
        }

//        val pauseSignal = Observable.create<Boolean> { emitter ->
//            pauseButton.setOnClickListener {
//                emitter.onNext(true)
//            }
//        }
//
//        val resumeSignal = Observable.create<Boolean> { emitter ->
//            resumeButton.setOnClickListener {
//                emitter.onNext(true)
//            }
//        }
    }

    override fun cannotRetry(error: Throwable, decision: Consumer<RxMethod.Event>) {
        setResultOnTextView(error.message ?: "Error")
        retryButton.setOnClickListener {
            decision.accept(RxMethod.Event.RETRY)
        }

        nextButton.setOnClickListener {
            decision.accept(RxMethod.Event.NEXT)
        }

        cancelButton.setOnClickListener {
            decision.accept(RxMethod.Event.CANCEL)
        }
    }

    private fun clearUi() {
        progressBar.max = 0
        progressBar.progress = 0
        progressPercentTextView.text = "0%"
        resultTextView.text = ""
    }

    private fun setResultOnTextView(text: String) {
        resultTextView.text = "$text\n----------------------------"
    }

    override fun onDestroy() {
        super.onDestroy()
        rxExecutor.shutdown()
    }
}
