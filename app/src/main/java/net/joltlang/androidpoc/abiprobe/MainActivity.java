package net.joltlang.androidpoc.abiprobe;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class MainActivity extends Activity {
  static {
    System.loadLibrary("chez_probe");
  }

  private static native int nativeChezAnswer(String petiteBootPath, String schemeBootPath);

  private File copyAsset(String name) throws IOException {
    File destination = new File(getFilesDir(), name);
    try (InputStream source = getAssets().open("chez/" + name);
         FileOutputStream output = new FileOutputStream(destination)) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = source.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
    }
    return destination;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);
    try {
      File petiteBoot = copyAsset("petite.boot");
      File schemeBoot = copyAsset("scheme.boot");
      result.setText("Chez (+ 40 2) = " + nativeChezAnswer(
          petiteBoot.getAbsolutePath(), schemeBoot.getAbsolutePath()));
    } catch (IOException exception) {
      result.setText("boot asset failure: " + exception.getMessage());
    }
    setContentView(result);
  }
}
