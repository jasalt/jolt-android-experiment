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
    runtime.dispatch("{:type :counter/inc}", increment ->
        runtime.dispatch("{:type :counter/dec}", decrement ->
            runtime.dispatch("not EDN", malformed -> {
              result.setText("Jolt dispatches = " + increment + " then " + decrement
                  + "; malformed = " + malformed);
              runtime.close();
            })));
  }

  @Override
  protected void onDestroy() {
    if (runtime != null) {
      runtime.close();
    }
    super.onDestroy();
  }
}
