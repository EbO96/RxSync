package pl.ebo96.rxsyncexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import pl.ebo96.rxsync.sync.event.*
import pl.ebo96.rxsync.sync.executor.RxExecutor
import pl.ebo96.rxsync.sync.method.MethodResult
import pl.ebo96.rxsync.sync.module.RxModule

class MainActivity : AppCompatActivity(), RxMethodEventHandler {

    private lateinit var rxExecutor: RxExecutor<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxExecutor = RxExecutor.Builder<Any>()
                .register(ExampleModule())
                .register(ExampleModule2())
                .setResultListener(object : RxResultListener<Any> {
                    override fun onNextResult(data: MethodResult<out Any>) {
                        RestApi.results.add("${data.result}")
                    }

                    override fun onNextUiResult(data: MethodResult<out Any>) {
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

                    override fun onModuleProgress(module: RxModule<*>, rxProgress: RxProgress) {
                    }

                    override fun onModulesRegistered(modules: PreparedModules) {
                        modules
                    }

                    override fun completed(rxProgress: RxProgress) {
                        RestApi.results.clear()
                        setResultOnTextView("Completed | $rxProgress")
                    }
                })
                .setElapsedTimeListener(object : RxElapsedTimeListener {
                    override fun elapsed(seconds: Long) {
                        elapsedTimeTextView.text = "$seconds [s]"
                    }
                })
                .setErrorListener(object : RxErrorListener {
                    override fun onError(error: Throwable) {
                        setResultOnTextView("$error on thread -> ${Thread.currentThread().name}")
                    }
                })
                .setRxScreen(object : RxActivityScreen {
                    override fun getSubject(): AppCompatActivity? {
                        return this@MainActivity
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

    override fun onNewRxEvent(error: Throwable, rxMethodEventConsumer: RxMethodEventConsumer) {
        setResultOnTextView("EVENT -> $error on thread -> ${Thread.currentThread().name}")
        retryButton.setOnClickListener {
            rxMethodEventConsumer.onResponse(RxMethodEvent.RETRY)
        }

        nextButton.setOnClickListener {
            rxMethodEventConsumer.onResponse(RxMethodEvent.NEXT)
        }

        cancelButton.setOnClickListener {
            rxMethodEventConsumer.onResponse(RxMethodEvent.CANCEL)
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
