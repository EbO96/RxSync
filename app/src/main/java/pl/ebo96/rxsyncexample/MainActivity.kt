package pl.ebo96.rxsyncexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsyncexample.sync.RxMethod
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class MainActivity : AppCompatActivity(), RxExecutor.RxEvent {

    private lateinit var rxExecutor: RxExecutor<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxExecutor = RxExecutor.Builder<Any>()
                .register(ExampleModule2())
                .register(ExampleModule(this))
                .setProgressHandler(Consumer {
                    progressBar.max = it.total
                    progressBar.progress = it.done
                    progressPercentTextView.text = "${it.percentage}%"
                    setResultOnTextView("$it")
                })
                .setErrorHandler(Consumer {
                    it.printStackTrace()
                    setResultOnTextView("${it.message}")
                })
                .build()

        startButton.setOnClickListener {
            clearUi()
            rxExecutor.start()
        }

        stopButton.setOnClickListener {
            clearUi()
            rxExecutor.stop()
        }
    }

    override fun onEvent(error: Throwable, event: Consumer<RxMethod.Event>) {
        setResultOnTextView(error.message ?: "Error")
        retryButton.setOnClickListener {
            event.accept(RxMethod.Event.RETRY)
        }

        nextButton.setOnClickListener {
            event.accept(RxMethod.Event.NEXT)
        }

        cancelButton.setOnClickListener {
            event.accept(RxMethod.Event.CANCEL)
        }
    }

    private fun clearUi() {
        progressBar.max = 0
        progressBar.progress = 0
        progressPercentTextView.text = "0%"
        resultTextView.text = ""
    }

    private fun setResultOnTextView(text: String) {
        resultTextView.text = "${resultTextView.text}\n$text\n----------------------------"
        Log.d(RxExecutor.TAG, text)
    }
}
