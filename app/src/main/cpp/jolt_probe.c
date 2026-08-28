#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef int (*poc_answer_fn)(void);

JNIEXPORT jint JNICALL
Java_net_joltlang_androidpoc_abiprobe_MainActivity_nativeJoltAnswer(
    JNIEnv *environment, jclass clazz) {
  (void)environment;
  (void)clazz;

  void *library = dlopen("libjoltpoc.so", RTLD_NOW | RTLD_LOCAL);
  if (library == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "dlopen failed: %s", dlerror());
    return -1;
  }

  jolt_init_fn init = (jolt_init_fn)dlsym(library, "jolt_library_init");
  jolt_lookup_fn lookup = (jolt_lookup_fn)dlsym(library, "jolt_lookup");
  if (init == NULL || lookup == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "Jolt ABI symbols missing: %s", dlerror());
    return -2;
  }
  if (init(0, NULL) != 0) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "jolt_library_init failed");
    return -3;
  }

  poc_answer_fn answer = (poc_answer_fn)lookup("poc_answer");
  if (answer == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "poc_answer export missing");
    return -4;
  }
  const int result = answer();
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "poc_answer() = %d", result);
  return result;
}
