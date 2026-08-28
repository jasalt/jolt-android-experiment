package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
  static {
    System.loadLibrary("abi_probe");
  }

  private static native int nativeAnswer();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);
    result.setText("poc_answer() = " + nativeAnswer());
    setContentView(result);
  }
}
