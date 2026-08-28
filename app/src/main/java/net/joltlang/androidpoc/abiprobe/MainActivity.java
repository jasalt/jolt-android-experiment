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
    runtime.dispatch("{:type :counter/inc}", valid -> {
      result.setText("Jolt dispatch = " + valid);
      // This UI-thread callback queues malformed input; it never enters JNI directly.
      runtime.dispatch("not EDN", malformed -> result.setText(
          "Jolt dispatch = " + valid + "; malformed = " + malformed));
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
