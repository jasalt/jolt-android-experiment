#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*poc_dispatch_counter_fn)(const char *);

static pthread_mutex_t lifecycle_lock = PTHREAD_MUTEX_INITIALIZER;
static pthread_t owner_thread;
static bool lifecycle_started;
static bool lifecycle_finished;
static jolt_shutdown_fn shutdown_runtime;
static poc_dispatch_counter_fn dispatch_counter;

static jstring result_string(JNIEnv *environment, const char *text) {
  return (*environment)->NewStringUTF(environment, text);
}

static bool valid_event(const char *event) {
  return strcmp(event, "{:type :counter/inc}") == 0 ||
      strcmp(event, "{:type :counter/dec}") == 0;
}

static const char *ensure_session(void) {
  pthread_mutex_lock(&lifecycle_lock);
  if (lifecycle_finished) {
    pthread_mutex_unlock(&lifecycle_lock);
    return "{:error :closed}";
  }
  if (lifecycle_started) {
    const bool owner = pthread_equal(pthread_self(), owner_thread);
    pthread_mutex_unlock(&lifecycle_lock);
    return owner ? NULL : "{:error :wrong-thread}";
  }
  lifecycle_started = true;
  owner_thread = pthread_self();
  pthread_mutex_unlock(&lifecycle_lock);

  void *library = dlopen("libjoltpoc.so", RTLD_NOW | RTLD_LOCAL);
  if (library == NULL) return "{:error :dlopen}";
  jolt_init_fn init = (jolt_init_fn)dlsym(library, "jolt_library_init");
  jolt_lookup_fn lookup = (jolt_lookup_fn)dlsym(library, "jolt_lookup");
  shutdown_runtime = (jolt_shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (init == NULL || lookup == NULL || shutdown_runtime == NULL || init(0, NULL) != 0) {
    return "{:error :initialization}";
  }
  dispatch_counter = (poc_dispatch_counter_fn)lookup("poc_dispatch_counter");
  if (dispatch_counter == NULL) {
    shutdown_runtime();
    return "{:error :exports}";
  }
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "owner JNI session initialized");
  return NULL;
}

JNIEXPORT jstring JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeJoltDispatch(
    JNIEnv *environment, jobject runtime, jstring event_edn) {
  (void)runtime;
  const char *event = (*environment)->GetStringUTFChars(environment, event_edn, NULL);
  if (event == NULL) return result_string(environment, "{:error :invalid-input}");
  if (!valid_event(event)) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "malformed event rejected");
    return result_string(environment, "{:error :invalid-event}");
  }

  const char *session_error = ensure_session();
  if (session_error != NULL) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "%s", session_error);
    return result_string(environment, session_error);
  }

  const int counter = dispatch_counter(event);
  (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
  char output[96];
  const int written = snprintf(output, sizeof output,
      "{:model {:counter %d, :events [], :platform nil}, :effects []}", counter);
  if (written < 0 || written >= (int)sizeof output) return result_string(environment, "{:error :output-too-large}");
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "dispatch counter=%d", counter);
  return result_string(environment, output);
}

JNIEXPORT void JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeJoltShutdown(
    JNIEnv *environment, jobject runtime) {
  (void)environment;
  (void)runtime;
  pthread_mutex_lock(&lifecycle_lock);
  if (!lifecycle_started || lifecycle_finished || !pthread_equal(pthread_self(), owner_thread)) {
    pthread_mutex_unlock(&lifecycle_lock);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "shutdown rejected");
    return;
  }
  lifecycle_finished = true;
  pthread_mutex_unlock(&lifecycle_lock);
  shutdown_runtime();
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "owner JNI session shutdown");
}
