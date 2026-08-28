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
typedef void (*jolt_collect_fn)(void);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*poc_answer_fn)(void);
typedef int (*poc_allocate_fn)(int);
typedef int (*poc_dispatch_counter_fn)(const char *);

static pthread_mutex_t lifecycle_lock = PTHREAD_MUTEX_INITIALIZER;
static pthread_t owner_thread;
static bool lifecycle_started;
static bool lifecycle_finished;

static void *wrong_thread_attempt(void *argument) {
  const pthread_t *owner = argument;
  if (!pthread_equal(pthread_self(), *owner)) {
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe",
        "wrong-thread Jolt access rejected before ABI call");
    return (void *)(intptr_t)1;
  }
  return NULL;
}

static jstring result_string(JNIEnv *environment, const char *text) {
  return (*environment)->NewStringUTF(environment, text);
}

JNIEXPORT jstring JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeJoltDispatch(
    JNIEnv *environment, jobject runtime, jstring event_edn) {
  (void)runtime;
  const char *event = (*environment)->GetStringUTFChars(environment, event_edn, NULL);
  if (event == NULL) return result_string(environment, "{:error :invalid-input}");

  // This reduced schema is deliberately validated before Jolt entry. It proves
  // malformed input is rejected without relying on an undocumented exception ABI.
  if (strcmp(event, "{:type :counter/inc}") != 0) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "malformed event rejected");
    return result_string(environment, "{:error :invalid-event}");
  }

  pthread_mutex_lock(&lifecycle_lock);
  if (lifecycle_started || lifecycle_finished) {
    const bool owner = pthread_equal(pthread_self(), owner_thread);
    pthread_mutex_unlock(&lifecycle_lock);
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe",
        owner ? "repeat init rejected" : "non-owner JNI entry rejected");
    return result_string(environment, owner ? "{:error :repeat-init}" : "{:error :wrong-thread}");
  }
  lifecycle_started = true;
  owner_thread = pthread_self();
  pthread_mutex_unlock(&lifecycle_lock);
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "owner JNI thread recorded");

  void *library = dlopen("libjoltpoc.so", RTLD_NOW | RTLD_LOCAL);
  if (library == NULL) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    return result_string(environment, "{:error :dlopen}");
  }
  jolt_init_fn init = (jolt_init_fn)dlsym(library, "jolt_library_init");
  jolt_lookup_fn lookup = (jolt_lookup_fn)dlsym(library, "jolt_lookup");
  jolt_collect_fn collect = (jolt_collect_fn)dlsym(library, "jolt_library_collect");
  jolt_shutdown_fn shutdown = (jolt_shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (init == NULL || lookup == NULL || collect == NULL || shutdown == NULL || init(0, NULL) != 0) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    return result_string(environment, "{:error :initialization}");
  }

  poc_answer_fn answer = (poc_answer_fn)lookup("poc_answer");
  poc_allocate_fn allocate = (poc_allocate_fn)lookup("poc_allocate");
  poc_dispatch_counter_fn dispatch = (poc_dispatch_counter_fn)lookup("poc_dispatch_counter");
  if (answer == NULL || allocate == NULL || dispatch == NULL) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    shutdown();
    return result_string(environment, "{:error :exports}");
  }
  for (int i = 0; i < 10000; ++i) {
    if (answer() != 42) {
      (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
      shutdown();
      return result_string(environment, "{:error :answer}");
    }
  }
  if (allocate(100000) != 100000) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    shutdown();
    return result_string(environment, "{:error :allocation}");
  }
  collect();
  if (answer() != 42) {
    (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
    shutdown();
    return result_string(environment, "{:error :collection}");
  }

  const int counter = dispatch(event);
  (*environment)->ReleaseStringUTFChars(environment, event_edn, event);
  pthread_t foreign_thread;
  if (pthread_create(&foreign_thread, NULL, wrong_thread_attempt, &owner_thread) != 0) {
    shutdown();
    return result_string(environment, "{:error :thread}");
  }
  void *foreign_result = NULL;
  if (pthread_join(foreign_thread, &foreign_result) != 0 || (intptr_t)foreign_result != 1) {
    shutdown();
    return result_string(environment, "{:error :thread}");
  }

  // Output is rendered into a fixed C-owned buffer and copied by NewStringUTF;
  // no pointer into Jolt-managed memory crosses the JNI boundary.
  char output[96];
  const int written = snprintf(output, sizeof output,
      "{:model {:counter %d, :events [], :platform nil}, :effects []}", counter);
  if (written < 0 || written >= (int)sizeof output) {
    shutdown();
    return result_string(environment, "{:error :output-too-large}");
  }
  shutdown();
  pthread_mutex_lock(&lifecycle_lock);
  lifecycle_finished = true;
  pthread_mutex_unlock(&lifecycle_lock);
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe",
      "dispatch counter=%d calls=10000 allocation=100000 compact=ok shutdown=ok", counter);
  return result_string(environment, output);
}
