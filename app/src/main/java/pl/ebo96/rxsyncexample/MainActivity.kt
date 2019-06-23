package pl.ebo96.rxsyncexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsyncexample.sync.event.RxEvent
import pl.ebo96.rxsyncexample.sync.event.RxMethodResultListener
import pl.ebo96.rxsyncexample.sync.executor.RxExecutor

class MainActivity : AppCompatActivity(), RxExecutor.RxEventHandler {

    private lateinit var rxExecutor: RxExecutor<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxExecutor = RxExecutor.Builder<Any>()
                .register(ExampleModule2())
                .register(ExampleModule())
                .setMethodResultListener(object : RxMethodResultListener<Any> {
                    override fun onResult(data: Any?) {
                        Log.d(RxExecutor.TAG, "on result -> $data, thread -> ${Thread.currentThread().name}")
                    }

                    override fun onUiResult(data: Any?) {
                        setResultOnTextView("$data")
                    }
                })
                .setProgressListener(Consumer {
                    progressBar.max = it.total
                    progressBar.progress = it.done
                    progressPercentTextView.text = "${it.percentage}%"
                    setResultOnTextView("$it")
                })
                .setErrorHandler(Consumer {
                    it.printStackTrace()
                    setResultOnTextView("${it.message}")
                })
                .setEventHandler(this)
                .build()

        startButton.setOnClickListener {
            clearUi()
            rxExecutor.start()
        }

        cancelAllButton.setOnClickListener {
            clearUi()
            rxExecutor.cancel()
        }
    }

    override fun onNewRxEvent(error: Throwable, rxEvent: Consumer<RxEvent>) {
        setResultOnTextView(error.message ?: "Error")
        retryButton.setOnClickListener {
            rxEvent.accept(RxEvent.RETRY)
        }

        nextButton.setOnClickListener {
            rxEvent.accept(RxEvent.NEXT)
        }

        cancelButton.setOnClickListener {
            rxEvent.accept(RxEvent.CANCEL)
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
