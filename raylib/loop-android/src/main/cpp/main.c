#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include "voxel_sensor.h"

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
  int sensor_available = voxel_sensor_start();
  float sensor_quaternion[4] = {0};
  long long sensor_timestamp = 0;
  int sensor_sample = 0;
  for (int attempt = 0; attempt < 25 && !sensor_sample; attempt++) {
    sensor_sample = voxel_sensor_poll(sensor_quaternion, &sensor_timestamp);
    if (!sensor_sample) usleep(10000);
  }
  LOGI("voxel sensor probe available=%d sample=%d timestamp=%lld q=%f,%f,%f,%f thread=%d",
       sensor_available, sensor_sample, sensor_timestamp,
       sensor_quaternion[0], sensor_quaternion[1], sensor_quaternion[2],
       sensor_quaternion[3], thread_id());
  voxel_sensor_stop();
  int socket_fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
  LOGI("native socket probe fd=%d errno=%d", socket_fd, errno);
  if (socket_fd >= 0) close(socket_fd);
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

#ifdef NDEBUG
  const char *loop_name = "raylib_gallery";
  const char *build_mode = "release";
#else
  const char *loop_name = "raylib_gallery_debug";
  const char *build_mode = "debug-nrepl";
#endif
#ifdef NDEBUG
  loop_fn debug_loop = (loop_fn)lookup("raylib_gallery_debug");
  LOGI("jolt_lookup raylib_gallery_debug=%s mode=release thread=%d owner=%d",
       debug_loop ? "unexpected" : "missing", thread_id(), owner);
  if (debug_loop) {
    shutdown();
    dlclose(library);
    return 6;
  }
#endif
  loop_fn loop = (loop_fn)lookup(loop_name);
  LOGI("jolt_lookup %s=%s mode=%s thread=%d owner=%d", loop_name,
       loop ? "ok" : "missing", build_mode, thread_id(), owner);
  if (!loop) {
    shutdown();
    dlclose(library);
    return 4;
  }

  LOGI("calling %s mode=%s thread=%d owner=%d", loop_name, build_mode,
       thread_id(), owner);
  int frames = loop();
  LOGI("%s returned result=%d mode=%s thread=%d owner=%d", loop_name, frames,
       build_mode, thread_id(), owner);
  shutdown();
  LOGI("jolt_library_shutdown thread=%d owner=%d", thread_id(), owner);
  dlclose(library);
  LOGI("gallery bootstrap complete thread=%d owner=%d", thread_id(), owner);
  return frames > 0 ? 0 : 5;
}
