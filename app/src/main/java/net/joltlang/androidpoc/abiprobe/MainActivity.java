package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private JoltRuntime runtime;
  private TextView result;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    result = new TextView(this);
    setContentView(result);
    runtime = new JoltRuntime();
    runtime.dispatch("{:type :lifecycle/create}", ignored -> { });
  }

  @Override
  protected void onStart() {
    super.onStart();
    runtime.dispatch("{:type :lifecycle/start}", ignored -> { });
  }

  @Override
  protected void onResume() {
    super.onResume();
    runtime.dispatch("{:type :lifecycle/resume}", model -> result.setText(
        "Runtime: initialized\n"
            + "Process ABI: " + Build.SUPPORTED_ABIS[0] + "\n"
            + "PID: " + Process.myPid() + "\n"
            + "Jolt thread: HandlerThread(JoltRuntime)\n"
            + "Lifecycle model: " + model));
  }

  @Override
  protected void onDestroy() {
    if (runtime != null) runtime.close();
    super.onDestroy();
  }
}
