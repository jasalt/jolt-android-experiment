package net.joltlang.androidpoc.abiprobe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  private lateinit var runtime: JoltRuntime
  private var debugEvalServer: DebugEvalServer? = null
  private var output by mutableStateOf("Starting Jolt runtime…")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    runtime = JoltRuntime()
    if (BuildConfig.DEBUG) try { debugEvalServer = DebugEvalServer(runtime) } catch (_: Exception) { }
    setContent { DiagnosticApp() }
    dispatch(":lifecycle/create")
  }

  private fun dispatch(type: String) = runtime.dispatch("{:type $type}") {
    output = it
    when {
      it.contains(":platform/vibrate") -> {
        (getSystemService(Vibrator::class.java)).vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        output = "$it\nVibration executed"
      }
      it.contains(":platform/open-uri") -> {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jolt-lang.net")))
        output = "$it\nURL intent launched"
      }
      it.contains(":platform/read-info") -> output = "$it\nLocale: ${Locale.getDefault()}\nPackage: $packageName"
      it.contains(":permission/request") -> requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
      it.contains(":notification/show") -> {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("jolt", "Jolt", NotificationManager.IMPORTANCE_DEFAULT))
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
          manager.notify(7, NotificationCompat.Builder(this, "jolt").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Jolt").setContentText("Counter notification").setAutoCancel(true).build())
          output = "$it\nNotification posted"
        } else output = "$it\nNotification denied"
      }
    }
  }

  @androidx.compose.runtime.Composable
  private fun DiagnosticApp() {
    var screen by remember { mutableStateOf("Runtime") }
    val screens = listOf("Runtime", "State", "Lifecycle", "Effects", "Permission", "Persistence", "Worker")
    Scaffold(bottomBar = { NavigationBar { screens.forEach { name ->
      NavigationBarItem(selected = screen == name, onClick = { screen = name }, icon = {}, label = { Text(name) })
    } } }) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text(screen)
        Text(output)
        when (screen) {
          "Runtime" -> { Text(runtime.diagnostics()); Button(onClick = { dispatch(":lifecycle/resume") }) { Text("Refresh runtime") } }
          "State" -> listOf(":counter/inc" to "Increment", ":counter/dec" to "Decrement", ":counter/reset" to "Reset").forEach { (event, label) -> Button(onClick = { dispatch(event) }) { Text(label) } }
          "Lifecycle" -> listOf(":lifecycle/create", ":lifecycle/start", ":lifecycle/resume", ":lifecycle/pause", ":lifecycle/stop").forEach { event -> Button(onClick = { dispatch(event) }) { Text(event) } }
          "Effects" -> {
            Button(onClick = { dispatch(":platform/copy-counter") }) { Text("Copy counter") }
            Button(onClick = { dispatch(":platform/vibrate") }) { Text("Vibrate") }
            Button(onClick = { dispatch(":platform/open-url") }) { Text("Open Jolt URL") }
            Button(onClick = { dispatch(":platform/read-info") }) { Text("Read locale/package") }
            Button(onClick = { dispatch(":platform/notify-counter") }) { Text("Show notification") }
          }
          "Permission" -> Button(onClick = { dispatch(":permission/request-notifications") }) { Text("Request notifications") }
          "Persistence" -> Button(onClick = { dispatch(":storage/restore :value 0") }) { Text("Restore counter") }
          "Worker" -> Button(onClick = { dispatch(":worker/completed") }) { Text("Complete worker") }
        }
      }
    }
  }

  override fun onStart() { super.onStart(); dispatch(":lifecycle/start") }
  override fun onResume() { super.onResume(); dispatch(":lifecycle/resume") }
  override fun onPause() { dispatch(":lifecycle/pause"); super.onPause() }
  override fun onStop() { dispatch(":lifecycle/stop"); super.onStop() }
  override fun onDestroy() { debugEvalServer?.close(); runtime.close(); super.onDestroy() }
}
