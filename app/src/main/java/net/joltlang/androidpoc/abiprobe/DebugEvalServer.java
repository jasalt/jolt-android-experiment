package net.joltlang.androidpoc.abiprobe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Debug-only loopback line protocol; transport never enters JNI directly. */
final class DebugEvalServer implements AutoCloseable {
  static final int PORT = 45678;
  private final JoltRuntime runtime;
  private final ServerSocket server;
  private final Thread thread;
  private volatile boolean running = true;

  DebugEvalServer(JoltRuntime runtime) throws Exception {
    this.runtime = runtime;
    server = new ServerSocket(PORT, 8, InetAddress.getLoopbackAddress());
    thread = new Thread(this::serve, "JoltDebugEvalServer");
    thread.start();
  }

  private void serve() {
    while (running) try (Socket socket = server.accept();
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
      String form = input.readLine();
      String response = evaluate(form);
      output.write(response);
      output.write('\n');
      output.flush();
    } catch (Exception ignored) {
      // Closing the listening socket stops the loop; a malformed/disconnected
      // client is isolated and cannot poison JoltRuntime.
    }
  }

  private String evaluate(String form) throws InterruptedException {
    if (form == null) return "{:error {:type :eval/malformed-frame}}";
    CountDownLatch done = new CountDownLatch(1);
    String[] result = {"{:error {:type :eval/timeout}}"};
    runtime.eval(form, value -> { result[0] = value; done.countDown(); });
    return done.await(10, TimeUnit.SECONDS) ? result[0] : "{:error {:type :eval/timeout}}";
  }

  @Override public void close() throws Exception { running = false; server.close(); thread.join(1000); }
}
