package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private JoltRuntime runtime;
  private TextView lifecycle;
  private TextView model;
  private TextView effect;
  private SharedPreferences preferences;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    lifecycle = new TextView(this);
    model = new TextView(this);
    effect = new TextView(this);
    layout.addView(lifecycle);
    layout.addView(model);
    layout.addView(effect);
    layout.addView(button("Increment", "{:type :counter/inc}"));
    layout.addView(button("Decrement", "{:type :counter/dec}"));
    layout.addView(button("Reset", "{:type :counter/reset}"));
    layout.addView(button("Copy counter", "{:type :platform/copy-counter}"));
    setContentView(layout);

    preferences = getSharedPreferences("jolt-poc", MODE_PRIVATE);
    runtime = new JoltRuntime();
    runtime.dispatch("{:type :lifecycle/create}", ignored -> { });
    if (preferences.contains("counter")) {
      int saved = preferences.getInt("counter", 0);
      runtime.dispatch("{:type :storage/restore :value " + saved + "}", result -> {
        model.setText("Jolt model: " + result);
        effect.setText("Storage restored: " + saved);
      });
    }
  }

  private Button button(String label, String event) {
    Button button = new Button(this);
    button.setText(label);
    button.setOnClickListener(view -> dispatch(event));
    return button;
  }

  private void dispatch(String event) {
    runtime.dispatch(event, result -> {
      model.setText("Jolt model: " + result);
      if (result.contains(":platform/clipboard")) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String text = "Jolt counter: " + counterFrom(result);
        clipboard.setPrimaryClip(ClipData.newPlainText("Jolt counter", text));
        CharSequence copied = clipboard.getPrimaryClip().getItemAt(0).getText();
        effect.setText("Clipboard effect: " + copied);
      } else if (result.contains(":storage/write")) {
        int counter = Integer.parseInt(counterFrom(result));
        preferences.edit().putInt("counter", counter).commit();
        effect.setText("Storage written: " + counter);
      }
    });
  }

  private static String counterFrom(String result) {
    String marker = ":counter ";
    int start = result.indexOf(marker) + marker.length();
    int end = result.indexOf(',', start);
    return result.substring(start, end);
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
