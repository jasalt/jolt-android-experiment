package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private JoltRuntime runtime;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);
    setContentView(result);

    runtime = new JoltRuntime();
    runtime.stress(first -> {
      result.setText("Jolt runtime stress = " + first);
      // This main-thread callback can only enqueue work; it never calls JNI.
      runtime.stress(repeated -> result.setText(
          "Jolt runtime stress = " + first + "; repeat init = " + repeated));
    });
  }

  @Override
  protected void onDestroy() {
    if (runtime != null) {
      runtime.close();
    }
    super.onDestroy();
  }
}
