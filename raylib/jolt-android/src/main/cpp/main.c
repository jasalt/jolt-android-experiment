#include <android/log.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <unistd.h>

#define LOG_TAG "jolt_raylib_bootstrap"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*raylib_host_noop_fn)(void);

static pid_t thread_id(void) { return gettid(); }

int main(int argc, char *argv[]) {
  pid_t owner = thread_id();
  LOGI("enter main thread=%d", owner);

  void *library = dlopen("libjoltraylib.so", RTLD_NOW | RTLD_LOCAL);
  if (!library) {
    LOGE("dlopen failed thread=%d error=%s", thread_id(), dlerror());
    return 1;
  }
  LOGI("dlopen ok thread=%d", thread_id());

  jolt_init_fn init = (jolt_init_fn)dlsym(library, "jolt_library_init");
  jolt_lookup_fn lookup = (jolt_lookup_fn)dlsym(library, "jolt_lookup");
  jolt_shutdown_fn shutdown =
      (jolt_shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (!init || !lookup || !shutdown) {
    LOGE("Jolt ABI lookup failed thread=%d", thread_id());
    dlclose(library);
    return 2;
  }

  int init_result = init(argc, argv);
  LOGI("jolt_library_init result=%d thread=%d owner=%d", init_result,
       thread_id(), owner);
  if (init_result != 0) {
    dlclose(library);
    return 3;
  }

  raylib_host_noop_fn noop =
      (raylib_host_noop_fn)lookup("raylib_host_noop");
  LOGI("jolt_lookup raylib_host_noop=%s thread=%d owner=%d",
       noop ? "ok" : "missing", thread_id(), owner);
  if (!noop) {
    shutdown();
    dlclose(library);
    return 4;
  }

  int result = noop();
  LOGI("raylib_host_noop result=%d thread=%d owner=%d", result, thread_id(),
       owner);
  shutdown();
  LOGI("jolt_library_shutdown thread=%d owner=%d", thread_id(), owner);
  dlclose(library);
  LOGI("bootstrap complete thread=%d owner=%d", thread_id(), owner);
  return result == 7 ? 0 : 5;
}
