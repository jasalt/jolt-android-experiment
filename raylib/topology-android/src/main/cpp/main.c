#include <android/log.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <unistd.h>

#define LOG_TAG "jolt_raylib_topology"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*screen_width_fn)(void);

static pid_t thread_id(void) { return gettid(); }

int main(int argc, char *argv[]) {
  pid_t owner = thread_id();
  LOGI("enter main thread=%d", owner);
  screen_width_fn direct_width =
      (screen_width_fn)dlsym(RTLD_DEFAULT, "GetScreenWidth");
  LOGI("direct process dlsym GetScreenWidth=%s thread=%d owner=%d",
       direct_width ? "ok" : "missing", thread_id(), owner);
  if (direct_width) {
    LOGI("direct GetScreenWidth result=%d thread=%d owner=%d",
         direct_width(), thread_id(), owner);
  }

  void *library = dlopen("libjoltraylib-topology.so", RTLD_NOW | RTLD_LOCAL);
  if (!library) {
    LOGE("dlopen failed thread=%d error=%s", thread_id(), dlerror());
    return 1;
  }
  LOGI("dlopen ok thread=%d owner=%d", thread_id(), owner);

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

  screen_width_fn width =
      (screen_width_fn)lookup("raylib_process_screen_width");
  LOGI("jolt_lookup raylib_process_screen_width=%s thread=%d owner=%d",
       width ? "ok" : "missing", thread_id(), owner);
  if (!width) {
    shutdown();
    dlclose(library);
    return 4;
  }

  int result = width();
  LOGI("GetScreenWidth via process lookup result=%d thread=%d owner=%d",
       result, thread_id(), owner);
  shutdown();
  LOGI("jolt_library_shutdown thread=%d owner=%d", thread_id(), owner);
  dlclose(library);
  LOGI("topology A complete thread=%d owner=%d", thread_id(), owner);
  return result == 0 ? 0 : 5;
}
