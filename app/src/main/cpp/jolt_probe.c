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
static poc_dispatch_counter_fn dispatch_counter;
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

static bool valid_event(const char *event) {
  int restored_counter;
  char trailing;
  return strcmp(event, "{:type :counter/inc}") == 0 ||
      strcmp(event, "{:type :counter/dec}") == 0 ||
      strcmp(event, "{:type :counter/reset}") == 0 ||
      strcmp(event, "{:type :platform/copy-counter}") == 0 ||
      strcmp(event, "{:type :platform/vibrate}") == 0 ||
      strcmp(event, "{:type :platform/open-url}") == 0 ||
      strcmp(event, "{:type :platform/read-info}") == 0 ||
      strcmp(event, "{:type :platform/notify-counter}") == 0 ||
      strcmp(event, "{:type :lifecycle/create}") == 0 ||
      strcmp(event, "{:type :lifecycle/start}") == 0 ||
      strcmp(event, "{:type :lifecycle/resume}") == 0 ||
      strcmp(event, "{:type :lifecycle/pause}") == 0 ||
      strcmp(event, "{:type :lifecycle/stop}") == 0 ||
      strcmp(event, "{:type :worker/completed}") == 0 ||
      strcmp(event, "{:type :permission/request-notifications}") == 0 ||
      strcmp(event, "{:type :permission/result-granted}") == 0 ||
      strcmp(event, "{:type :permission/result-denied}") == 0 ||
      sscanf(event, "{:type :storage/restore :value %d}%c", &restored_counter, &trailing) == 1;
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
  lifecycle_code = (poc_lifecycle_code_fn)lookup("poc_lifecycle_code");
  effect_code = (poc_effect_code_fn)lookup("poc_effect_code");
  worker_code = (poc_worker_code_fn)lookup("poc_worker_code");
  permission_code = (poc_permission_code_fn)lookup("poc_permission_code");
  debug_eval = (poc_debug_eval_fn)lookup("poc_debug_eval");
  if (dispatch_counter == NULL || lifecycle_code == NULL || effect_code == NULL || worker_code == NULL || permission_code == NULL || debug_eval == NULL) {
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
  const int lifecycle = lifecycle_code();
  const int effect = effect_code(event);
  const int worker = worker_code();
  const int permission = permission_code();
  (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
  char output[240];
  const int written = effect == 1
      ? snprintf(output, sizeof output,
          "{:model {:counter %d, :events [], :platform nil, :lifecycle %s, :worker %s, :notification-permission %s}, :effects [{:type :platform/clipboard, :text \"Jolt counter: %d\"}]}",
          counter, lifecycle_name(lifecycle), worker ? ":completed" : "nil", permission_name(permission), counter)
      : effect == 2
          ? snprintf(output, sizeof output,
              "{:model {:counter %d, :events [], :platform nil, :lifecycle %s, :worker %s, :notification-permission %s}, :effects [{:type :storage/write, :key \"counter\", :value %d}]}",
              counter, lifecycle_name(lifecycle), worker ? ":completed" : "nil", permission_name(permission), counter)
          : effect == 3
              ? snprintf(output, sizeof output,
                  "{:model {:counter %d, :events [], :platform nil, :lifecycle %s, :worker %s, :notification-permission %s}, :effects [{:type :permission/request, :permission :notifications}]}",
                  counter, lifecycle_name(lifecycle), worker ? ":completed" : "nil", permission_name(permission))
              : effect == 4
                  ? snprintf(output, sizeof output,
                      "{:model {:counter %d}, :effects [{:type :platform/vibrate, :duration-ms 50}]}", counter)
                  : effect == 5
                      ? snprintf(output, sizeof output,
                          "{:model {:counter %d}, :effects [{:type :platform/open-uri, :uri \"https://jolt-lang.net\"}]}", counter)
                      : effect == 6
                          ? snprintf(output, sizeof output,
                              "{:model {:counter %d}, :effects [{:type :platform/read-info}]}", counter)
                          : effect == 7
                              ? snprintf(output, sizeof output,
                                  "{:model {:counter %d}, :effects [{:type :notification/show, :title \"Jolt\", :body \"Counter: %d\"}]}", counter, counter)
                              : snprintf(output, sizeof output,
                              "{:model {:counter %d, :events [], :platform nil, :lifecycle %s, :worker %s, :notification-permission %s}, :effects []}",
                              counter, lifecycle_name(lifecycle), worker ? ":completed" : "nil", permission_name(permission));
  if (written < 0 || written >= (int)sizeof output) return result_string(environment, "{:error :output-too-large}");
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "dispatch counter=%d", counter);
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
