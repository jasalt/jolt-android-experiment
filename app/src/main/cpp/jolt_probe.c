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
typedef const char *(*poc_dispatch_fn)(const char *);
typedef int (*poc_lifecycle_code_fn)(void);
typedef int (*poc_effect_code_fn)(const char *);
typedef int (*poc_worker_code_fn)(void);
typedef int (*poc_permission_code_fn)(void);
typedef const char *(*poc_debug_eval_fn)(const char *);

static pthread_mutex_t lifecycle_lock = PTHREAD_MUTEX_INITIALIZER;
static pthread_t owner_thread;
static bool lifecycle_started;
static bool lifecycle_finished;
static jolt_shutdown_fn shutdown_runtime;
static poc_dispatch_fn dispatch;
static poc_lifecycle_code_fn lifecycle_code;
static poc_effect_code_fn effect_code;
static poc_worker_code_fn worker_code;
static poc_permission_code_fn permission_code;
static poc_debug_eval_fn debug_eval;

static jstring result_string(JNIEnv *environment, const char *text) {
  return (*environment)->NewStringUTF(environment, text);
}

static const char *lifecycle_name(int code) {
  switch (code) {
    case 1: return ":created";
    case 2: return ":started";
    case 3: return ":resumed";
    case 4: return ":paused";
    case 5: return ":stopped";
    default: return "nil";
  }
}

static const char *permission_name(int code) {
  switch (code) {
    case 1: return ":granted";
    case 2: return ":denied";
    default: return "nil";
  }
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
  dispatch = (poc_dispatch_fn)lookup("poc_dispatch");
  debug_eval = (poc_debug_eval_fn)lookup("poc_debug_eval");
  if (dispatch == NULL || debug_eval == NULL) {
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
  const char *session_error = ensure_session();
  if (session_error != NULL) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "%s", session_error);
    return result_string(environment, session_error);
  }

  // The Jolt export decodes and validates the event, advances the persistent
  // model, and serializes canonical EDN. :string returns a C-owned copy; JNI
  // immediately copies it into a Java String before another Jolt entry.
  const char *output = dispatch(event);
  (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
  if (output == NULL) return result_string(environment, "{:error :dispatch/no-result}");
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "canonical dispatch completed");
  return result_string(environment, output);
}

JNIEXPORT jstring JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeWrongThreadProbe(
    JNIEnv *environment, jobject runtime) {
  (void)runtime;
  const char *error = ensure_session();
  // Test-only probe: this must return :wrong-thread after the runtime thread
  // initialized the library, rather than entering any Jolt export.
  return result_string(environment, error == NULL ? "{:error :unexpected-owner}" : error);
}

JNIEXPORT jstring JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeJoltEval(
    JNIEnv *environment, jobject runtime, jstring source) {
  (void)runtime;
  const char *input = (*environment)->GetStringUTFChars(environment, source, NULL);
  if (input == NULL) return result_string(environment, "{:error {:type :eval/invalid-input}}");
  if (strlen(input) > 65536) {
    (*environment)->ReleaseStringUTFChars(environment, source, input);
    return result_string(environment, "{:error {:type :eval/input-too-large}}");
  }
  const char *session_error = ensure_session();
  if (session_error != NULL) {
    (*environment)->ReleaseStringUTFChars(environment, source, input);
    return result_string(environment, session_error);
  }
  const char *result = debug_eval(input);
  (*environment)->ReleaseStringUTFChars(environment, source, input);
  if (result == NULL) return result_string(environment, "{:error {:type :eval/no-result}}");
  // :string exports copy into the C ABI. JNI copies again into a Java String.
  return result_string(environment, result);
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
