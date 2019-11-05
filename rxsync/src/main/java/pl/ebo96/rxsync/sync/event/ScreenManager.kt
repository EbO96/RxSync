package pl.ebo96.rxsync.sync.event

import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class ScreenManager(private val rxScreen: RxScreen<*>) : LifecycleObserver {

    private var state: State = State.LetSleep
    private var onPauseState: State? = null

    private val window: Window?
        get() = when (val subject = rxScreen.getSubject()) {
            is Fragment -> {
                subject.activity?.window
            }
            is AppCompatActivity -> {
                subject.window
            }
            else -> throw Exception("Subject must be Fragment or Activity")
        }

    private val lifecycle: Lifecycle?
        get() = when (val subject = rxScreen.getSubject()) {
            is Fragment -> {
                subject.lifecycle
            }
            is AppCompatActivity -> {
                subject.lifecycle
            }
            else -> throw Exception("Subject must be Fragment or Activity")
        }

    fun keepAwake() {
        rxScreen.getSubject()?.apply {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)?.also {
                state = State.KeepAwake
                lifecycle?.addObserver(this@ScreenManager)
            }
        }
    }

    fun letSleep() {
        rxScreen.getSubject()?.apply {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)?.also {
                state = State.LetSleep
                lifecycle?.addObserver(this@ScreenManager)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        restoreState()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        onPauseState = state
        letSleep()
    }

    private fun restoreState() {
        when (onPauseState) {
            State.KeepAwake -> keepAwake()
            State.LetSleep -> letSleep()
        }
    }

    private sealed class State {
        object KeepAwake : State()
        object LetSleep : State()
    }
}