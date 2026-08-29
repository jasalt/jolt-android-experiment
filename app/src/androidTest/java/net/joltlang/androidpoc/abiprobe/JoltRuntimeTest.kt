package net.joltlang.androidpoc.abiprobe

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JoltRuntimeTest {
  private fun dispatch(runtime: JoltRuntime, event: String): String {
    val done = CountDownLatch(1)
    var result = ""
    runtime.dispatch(event) { value -> result = value; done.countDown() }
    assertTrue("dispatch timed out", done.await(15, TimeUnit.SECONDS))
    return result
  }

  private fun eval(runtime: JoltRuntime, source: String): String {
    val done = CountDownLatch(1)
    var result = ""
    runtime.eval(source) { value -> result = value; done.countDown() }
    assertTrue("eval timed out", done.await(20, TimeUnit.SECONDS))
    return result
  }

  @Test fun sharedCanonicalFixtureCorpus() {
    val runtime = JoltRuntime()
    try {
      val lines = InstrumentationRegistry.getInstrumentation().context.assets
        .open("fixtures.tsv").bufferedReader().readLines()
      lines.filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
        val fields = line.split("\t", limit = 3)
        assertEquals("fixture ${fields[0]}", fields[2], dispatch(runtime, fields[1]))
      }
    } finally {
      runtime.close()
    }
  }

  @Test fun queuedEntryWrongThreadRejectionAndRecovery() {
    val runtime = JoltRuntime()
    val duplicate = JoltRuntime()
    try {
      assertTrue(dispatch(runtime, "{:type :counter/inc}").contains(":counter 1"))
      // Called on the instrumentation thread, never JoltRuntime.
      assertEquals("{:error :wrong-thread}", runtime.wrongThreadProbe())
      // A second runtime owns a different HandlerThread and cannot initialize
      // or enter the process-global Jolt library.
      assertEquals("{:error :wrong-thread}", dispatch(duplicate, "{:type :counter/inc}"))
      assertTrue(dispatch(runtime, "{:type :counter/dec}").contains(":counter 0"))
      runStress(runtime)
    } finally {
      duplicate.close()
      runtime.close()
    }
  }

  private fun runStress(runtime: JoltRuntime) {
      val unicode = listOf("ASCII", "Suomi ääkkönen", "😀", "é", "👨‍👩‍👧‍👦")
      unicode.forEach { value ->
        assertTrue(eval(runtime, "(count ${quote(value)})").contains(":ok"))
      }
      // Allocation forces Jolt-managed values through repeated JNI string calls.
      repeat(100) { assertTrue(eval(runtime, "(count (vec (range 10000)))").contains(":ok 10000")) }
      listOf(1024, 64 * 1024, 1024 * 1024).forEach { bytes ->
        val source = "(count \"${"x".repeat(if (bytes >= 64 * 1024) bytes else bytes - 12)}\")"
        val result = eval(runtime, source)
        if (bytes >= 64 * 1024) assertTrue(result.contains(":eval/input-too-large"))
        else assertTrue(result.contains(":ok"))
      }
      assertTrue(dispatch(runtime, "{:type :counter/inc}").contains(":counter 1"))
  }

  private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
