#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <math.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include "voxel_sensor.h"
#include "voxel_orientation.h"
#include "raylib.h"

#ifdef VOXEL_BOX3D_PROBE
#include <stdint.h>
extern uint32_t vb3_world_create(double, double, double, int);
extern void vb3_world_step(uint32_t, float, int);
extern void vb3_world_destroy(uint32_t);
#endif

#define LOG_TAG "jolt_raylib_gallery"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef int (*jolt_init_fn)(int, char **);
typedef void *(*jolt_lookup_fn)(const char *);
typedef void (*jolt_shutdown_fn)(void);
typedef int (*loop_fn)(void);
typedef int (*probe_fn)(void);

static pid_t thread_id(void) { return gettid(); }

/*
 * The Jolt scene owns input and HUD state; this function owns no state and is
 * called only by that scene's Raylib owner thread between BeginDrawing and
 * EndDrawing. Keeping Camera3D and Vector3 here avoids passing Raylib ABI
 * aggregates through the Jolt FFI boundary.
 */
void voxel_draw_scene(int width, int height, float yaw, float pitch,
                      float destruction, float charge, int shots) {
  int portrait = height > width;
  float camera_radius = portrait ? 24.0f : 15.0f;
  float camera_height = (portrait ? 8.0f : 6.0f) + pitch * 5.0f;
  Camera3D camera = {0};
  camera.position = (Vector3){sinf(yaw) * camera_radius, camera_height,
                              cosf(yaw) * camera_radius};
  camera.target = (Vector3){0.0f, 2.0f, 0.0f};
  camera.up = (Vector3){0.0f, 1.0f, 0.0f};
  camera.fovy = portrait ? 62.0f : 45.0f;
  camera.projection = CAMERA_PERSPECTIVE;

  ClearBackground((Color){113, 188, 235, 255});
  BeginMode3D(camera);
  DrawPlane((Vector3){0.0f, -0.51f, 0.0f}, (Vector2){28.0f, 28.0f},
            (Color){91, 141, 78, 255});
  DrawGrid(28, 1.0f);

  int cell_index = 0;
  const int cell_total = 7 * 5 * 3;
  const int destroyed = (int)(destruction * cell_total);
  for (int x = -3; x <= 3; x++) {
    for (int y = 0; y < 5; y++) {
      for (int z = -1; z <= 1; z++) {
        int solid = y == 0 || z == 0 || x == 0 || x == 3 || y == 4;
        if (!solid) continue;
        int remove = (cell_index++ % cell_total) < destroyed;
        if (remove) continue;
        Vector3 center = {(float)x, (float)y + 0.5f, (float)z};
        Color block = y == 0 ? (Color){110, 83, 54, 255}
                             : (Color){200, 163, 103, 255};
        DrawCube(center, 0.94f, 0.94f, 0.94f, block);
        DrawCubeWires(center, 0.95f, 0.95f, 0.95f,
                      (Color){67, 47, 30, 255});
      }
    }
  }

  DrawCube((Vector3){-6.0f, 0.25f, 4.0f}, 2.5f, 0.5f, 1.8f,
           (Color){76, 72, 67, 255});
  DrawSphere((Vector3){-5.2f, 1.05f + charge * 0.5f, 3.6f},
             0.28f + charge * 0.12f, (Color){42, 42, 47, 255});
  for (int shot = 0; shot < shots; shot++) {
    float progress = 2.0f + (float)shot * 0.8f;
    DrawSphere((Vector3){-5.0f + progress, 1.4f + progress * 0.30f,
                          3.4f - progress * 0.55f},
               0.13f, (Color){48, 45, 43, 255});
  }
  EndMode3D();
}

int voxel_asset_visual_probe(void) {
  Image image = LoadImage("raylib-gallery/voxel-marker.png");
  int image_ok = image.data != NULL && image.width == 8 && image.height == 8;
  if (image.data != NULL) UnloadImage(image);
  Font font = LoadFont("raylib-gallery/DroidSans.ttf");
  int font_ok = font.texture.id != 0 && font.baseSize > 0;
  if (font.texture.id != 0) UnloadFont(font);
  return image_ok && font_ok;
}

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
#ifdef VOXEL_ORIENTATION_PROBE
  int landscape_request = voxel_set_orientation(1);
  LOGI("voxel orientation probe landscape=%d thread=%d", landscape_request,
       thread_id());
  usleep(500000);
  int portrait_request = voxel_set_orientation(0);
  LOGI("voxel orientation probe portrait=%d thread=%d", portrait_request,
       thread_id());
#endif
#ifdef VOXEL_BOX3D_PROBE
  uint32_t probe_world = vb3_world_create(0.0, -9.8, 0.0, 1);
  for (int step = 0; step < 10; step++) vb3_world_step(probe_world, 0.016f, 1);
  LOGI("voxel Box3D probe world=%u steps=10 thread=%d", probe_world, thread_id());
  vb3_world_destroy(probe_world);
#endif
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
#ifdef VOXEL_ASSET_PROBE
  char *asset_text = LoadFileText("raylib-gallery/voxel-state.edn");
  int asset_loaded = asset_text != NULL;
  if (asset_text != NULL) UnloadFileText(asset_text);
  const char *state = "{:probe true}";
  int saved = SaveFileText("voxel-probe-state.edn", (char *)state);
  char *state_text = LoadFileText("voxel-probe-state.edn");
  int state_loaded = state_text != NULL;
  if (state_text != NULL) UnloadFileText(state_text);
  LOGI("voxel asset probe loaded=%d saved=%d writable-readback=%d thread=%d",
       asset_loaded, saved, state_loaded, thread_id());
#endif

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

#ifdef VOXEL_BOX3D_PROBE
  probe_fn voxel_ffi_probe = (probe_fn)lookup("raylib_voxel_native_probe");
  LOGI("jolt_lookup voxel native probe=%s thread=%d owner=%d",
       voxel_ffi_probe ? "ok" : "missing", thread_id(), owner);
  if (!voxel_ffi_probe) {
    shutdown();
    dlclose(library);
    return 7;
  }
  LOGI("Jolt FFI Box3D probe result=%d thread=%d owner=%d",
       voxel_ffi_probe(), thread_id(), owner);
#endif

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
