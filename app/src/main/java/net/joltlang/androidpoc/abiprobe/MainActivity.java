package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
  static {
    System.loadLibrary("jolt_probe");
  }

  private static native int nativeJoltStress();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);
    int first = nativeJoltStress();
    int repeatedInitialization = nativeJoltStress();
    result.setText("Jolt stress = " + first + "; repeat init = " + repeatedInitialization);
    setContentView(result);
  }
}
