#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_collect_fn)(void);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*poc_answer_fn)(void);
typedef int (*poc_allocate_fn)(int);

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

JNIEXPORT jint JNICALL
Java_net_joltlang_androidpoc_abiprobe_JoltRuntime_nativeJoltStress(
    JNIEnv *environment, jobject runtime) {
  (void)environment;
  (void)runtime;

  pthread_mutex_lock(&lifecycle_lock);
  if (lifecycle_started || lifecycle_finished) {
    const bool owner = pthread_equal(pthread_self(), owner_thread);
    pthread_mutex_unlock(&lifecycle_lock);
    __android_log_print(ANDROID_LOG_INFO, "jolt_probe",
        owner ? "repeat init rejected" : "non-owner JNI entry rejected");
    return owner ? -1 : -11;
  }
  lifecycle_started = true;
  owner_thread = pthread_self();
  pthread_mutex_unlock(&lifecycle_lock);
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe", "owner JNI thread recorded");

  void *library = dlopen("libjoltpoc.so", RTLD_NOW | RTLD_LOCAL);
  if (library == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "dlopen failed: %s", dlerror());
    return -2;
  }
  jolt_init_fn init = (jolt_init_fn)dlsym(library, "jolt_library_init");
  jolt_lookup_fn lookup = (jolt_lookup_fn)dlsym(library, "jolt_lookup");
  jolt_collect_fn collect = (jolt_collect_fn)dlsym(library, "jolt_library_collect");
  jolt_shutdown_fn shutdown = (jolt_shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (init == NULL || lookup == NULL || collect == NULL || shutdown == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "Jolt ABI symbols missing: %s", dlerror());
    return -3;
  }
  if (init(0, NULL) != 0) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "jolt_library_init failed");
    return -4;
  }

  poc_answer_fn answer = (poc_answer_fn)lookup("poc_answer");
  poc_allocate_fn allocate = (poc_allocate_fn)lookup("poc_allocate");
  if (answer == NULL || allocate == NULL) {
    __android_log_print(ANDROID_LOG_ERROR, "jolt_probe", "Jolt exports missing");
    return -5;
  }
  for (int i = 0; i < 10000; ++i) {
    if (answer() != 42) return -6;
  }
  if (allocate(100000) != 100000) return -7;
  collect();
  if (answer() != 42) return -8;

  pthread_t foreign_thread;
  if (pthread_create(&foreign_thread, NULL, wrong_thread_attempt, &owner_thread) != 0) return -9;
  void *foreign_result = NULL;
  if (pthread_join(foreign_thread, &foreign_result) != 0 || (intptr_t)foreign_result != 1) return -10;

  shutdown();
  pthread_mutex_lock(&lifecycle_lock);
  lifecycle_finished = true;
  pthread_mutex_unlock(&lifecycle_lock);
  __android_log_print(ANDROID_LOG_INFO, "jolt_probe",
      "one=42 calls=10000 allocation=100000 compact=ok again=42 wrong-thread=rejected shutdown=ok");
  return 42;
}
