package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private JoltRuntime runtime;
  private TextView lifecycle;
  private TextView model;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    lifecycle = new TextView(this);
    model = new TextView(this);
    layout.addView(lifecycle);
    layout.addView(model);
    layout.addView(button("Increment", "{:type :counter/inc}"));
    layout.addView(button("Decrement", "{:type :counter/dec}"));
    layout.addView(button("Reset", "{:type :counter/reset}"));
    setContentView(layout);

    runtime = new JoltRuntime();
    runtime.dispatch("{:type :lifecycle/create}", ignored -> { });
  }

  private Button button(String label, String event) {
    Button button = new Button(this);
    button.setText(label);
    button.setOnClickListener(view -> dispatch(event));
    return button;
  }

  private void dispatch(String event) {
    runtime.dispatch(event, result -> model.setText("Jolt model: " + result));
  }

  @Override
  protected void onStart() {
    super.onStart();
    runtime.dispatch("{:type :lifecycle/start}", ignored -> { });
  }

  @Override
  protected void onResume() {
    super.onResume();
    runtime.dispatch("{:type :lifecycle/resume}", result -> {
      lifecycle.setText("Runtime: initialized\n"
          + "Process ABI: " + Build.SUPPORTED_ABIS[0] + "\n"
          + "PID: " + Process.myPid() + "\n"
          + "Jolt thread: HandlerThread(JoltRuntime)\n"
          + "Lifecycle model: " + result);
      model.setText("Jolt model: " + result);
    });
  }

  @Override
  protected void onDestroy() {
    if (runtime != null) runtime.close();
    super.onDestroy();
  }
}
