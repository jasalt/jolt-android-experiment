#include <android/log.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <unistd.h>

#define LOG_TAG "jolt_raylib_gallery"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*loop_fn)(void);
typedef int (*probe_fn)(void);

static pid_t thread_id(void) { return gettid(); }

int main(int argc, char *argv[]) {
  pid_t owner = thread_id();
  LOGI("enter main thread=%d owner=%d", owner, owner);
  void *library = dlopen("libjoltraylib-loop.so", RTLD_NOW | RTLD_LOCAL);
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

  const char *probe_names[] = {
      "raylib_abi_constant_size", "raylib_abi_map_size",
      "raylib_abi_color_layout_map", "raylib_abi_color_layout_marker",
      "raylib_abi_color_raw_size", "raylib_abi_layout_size_bound",
      "raylib_abi_color_size"};
  for (size_t i = 0; i < sizeof(probe_names) / sizeof(probe_names[0]); i++) {
    probe_fn probe = (probe_fn)lookup(probe_names[i]);
    LOGI("aggregate probe lookup name=%s result=%s thread=%d owner=%d",
         probe_names[i], probe ? "ok" : "missing", thread_id(), owner);
    if (!probe) {
      shutdown();
      dlclose(library);
      return 4;
    }
    LOGI("aggregate probe call name=%s result=%d thread=%d owner=%d",
         probe_names[i], probe(), thread_id(), owner);
  }

  probe_fn abi_verify = (probe_fn)lookup("raylib_abi_verify");
  LOGI("jolt_lookup aggregate verify=%s thread=%d owner=%d",
       abi_verify ? "ok" : "missing", thread_id(), owner);
  if (!abi_verify) {
    shutdown();
    dlclose(library);
    return 4;
  }
  LOGI("aggregate matrix result=%d thread=%d owner=%d", abi_verify(),
       thread_id(), owner);

  loop_fn loop = (loop_fn)lookup("raylib_gallery");
  LOGI("jolt_lookup raylib_gallery=%s thread=%d owner=%d",
       loop ? "ok" : "missing", thread_id(), owner);
  if (!loop) {
    shutdown();
    dlclose(library);
    return 4;
  }

  int frames = loop();
  LOGI("raylib_gallery result=%d thread=%d owner=%d", frames,
       thread_id(), owner);
  shutdown();
  LOGI("jolt_library_shutdown thread=%d owner=%d", thread_id(), owner);
  dlclose(library);
  LOGI("gallery bootstrap complete thread=%d owner=%d", thread_id(), owner);
  return frames > 0 ? 0 : 5;
}
