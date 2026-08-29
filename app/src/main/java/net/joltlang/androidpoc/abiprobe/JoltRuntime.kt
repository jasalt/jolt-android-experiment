package net.joltlang.androidpoc.abiprobe

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.util.function.Consumer

class JoltRuntime {
  private val thread = HandlerThread("JoltRuntime").apply { start() }
  private val handler = Handler(thread.looper)
  private val mainHandler = Handler(Looper.getMainLooper())
  @Volatile private var roundTrips = 0L

  fun diagnostics(): String = "initialized=true\nJolt thread: ${thread.name}\nJolt thread ID: ${thread.id}\nJNI round trips: $roundTrips"

  /** Instrumentation-only proof that direct JNI entry is rejected. */
  fun wrongThreadProbe(): String = nativeWrongThreadProbe()

  /** Debug-only bounded evaluation; all entries remain on the Jolt owner thread. */
  fun eval(source: String, onComplete: Consumer<String>) {
    if (!BuildConfig.DEBUG) {
      onComplete.accept("{:error {:type :eval/disabled}}")
      return
    }
    handler.post {
      val result = nativeJoltEval(source)
      roundTrips += 1
      mainHandler.post { onComplete.accept(result) }
    }
  }

  fun dispatch(event: String, onComplete: Consumer<String>) {
    handler.post {
      Log.i(TAG, "entering JNI on ${Thread.currentThread().name}")
      val result = nativeJoltDispatch(event)
      roundTrips += 1
      mainHandler.post { onComplete.accept(result) }
    }
  }

  fun close() {
    handler.post {
      nativeJoltShutdown()
      thread.quitSafely()
    }
  }

  private external fun nativeJoltDispatch(event: String): String
  private external fun nativeWrongThreadProbe(): String
  private external fun nativeJoltEval(source: String): String
  private external fun nativeJoltShutdown()

  private companion object {
    const val TAG = "JoltRuntime"

    init {
      System.loadLibrary("jolt_probe")
    }
  }
}
