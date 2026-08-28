#include <jni.h>

int poc_answer(void) {
  return 42;
}

JNIEXPORT jint JNICALL
Java_net_joltlang_androidpoc_abiprobe_MainActivity_nativeAnswer(
    JNIEnv *environment,
    jclass clazz) {
  (void) environment;
  (void) clazz;
  return poc_answer();
}
