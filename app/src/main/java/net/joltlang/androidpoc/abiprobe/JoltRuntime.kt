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

  fun dispatch(event: String, onComplete: Consumer<String>) {
    handler.post {
      Log.i(TAG, "entering JNI on ${Thread.currentThread().name}")
      val result = nativeJoltDispatch(event)
      mainHandler.post { onComplete.accept(result) }
    }
  }

  fun close() {
    thread.quitSafely()
  }

  private external fun nativeJoltDispatch(event: String): String

  private companion object {
    const val TAG = "JoltRuntime"

    init {
      System.loadLibrary("jolt_probe")
    }
  }
}
