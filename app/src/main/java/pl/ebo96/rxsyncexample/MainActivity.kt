package pl.ebo96.rxsyncexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsync.sync.event.*
import pl.ebo96.rxsync.sync.executor.RxExecutor

class MainActivity : AppCompatActivity(), RxMethodEventHandler {

    private lateinit var rxExecutor: RxExecutor<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxExecutor = RxExecutor.Builder<Any>()
                .register(ExampleModule())
                .register(ExampleModule2())
                .setResultListener(object : RxResultListener<Any> {
                    override fun onNextResult(data: Any?) {
                        RestApi.results.add("$data")
                        Log.i("executor", "Result $data, on thread ${Thread.currentThread().name}")
                    }

                    override fun onNextUiResult(data: Any?) {
                        setResultOnTextView("$data")
                    }
                })
                .setProgressListener(object : RxProgressListener {
                    override fun onProgress(rxProgress: RxProgress) {
                        progressBar.max = rxProgress.total
                        progressBar.progress = rxProgress.done
                        progressPercentTextView.text = "${rxProgress.percentage}%"
                        setResultOnTextView("$rxProgress")
                    }

                    override fun completed() {
                        RestApi.results.clear()
                        setResultOnTextView("Completed")
                    }
                })
                .setErrorListener(object : RxErrorListener {
                    override fun onError(error: Throwable) {
                        error.printStackTrace()
                        setResultOnTextView("${error.message}")
                    }
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

    override fun onNewRxEvent(error: Throwable, rxMethodEvent: RxMethodEventConsumer) {
        setResultOnTextView(error.message ?: "Error")
        retryButton.setOnClickListener {
            rxMethodEvent.onResponse(RxMethodEvent.RETRY)
        }

        nextButton.setOnClickListener {
            rxMethodEvent.onResponse(RxMethodEvent.NEXT)
        }

        cancelButton.setOnClickListener {
            rxMethodEvent.onResponse(RxMethodEvent.CANCEL)
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
    }
}
