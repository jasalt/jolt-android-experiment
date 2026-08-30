#include <stddef.h>
#include <stdint.h>
#include "raylib.h"

/* Compile-time oracle for the pinned public Raylib ABI. */
#define ASSERT_SIZE(type, expected) _Static_assert(sizeof(type) == expected, #type " size")
#define ASSERT_OFFSET(type, field, expected) _Static_assert(offsetof(type, field) == expected, #type "." #field " offset")
ASSERT_SIZE(Color, 4);
ASSERT_OFFSET(Color, r, 0); ASSERT_OFFSET(Color, a, 3);
ASSERT_SIZE(Vector2, 8);
ASSERT_OFFSET(Vector2, x, 0); ASSERT_OFFSET(Vector2, y, 4);
ASSERT_SIZE(Vector3, 12);
ASSERT_OFFSET(Vector3, z, 8);
ASSERT_SIZE(Rectangle, 16);
ASSERT_OFFSET(Rectangle, width, 8); ASSERT_OFFSET(Rectangle, height, 12);
ASSERT_SIZE(Camera2D, 24);
ASSERT_OFFSET(Camera2D, offset, 0); ASSERT_OFFSET(Camera2D, target, 8);
ASSERT_OFFSET(Camera2D, rotation, 16); ASSERT_OFFSET(Camera2D, zoom, 20);
ASSERT_SIZE(Camera3D, 44);
ASSERT_OFFSET(Camera3D, target, 12); ASSERT_OFFSET(Camera3D, up, 24);
ASSERT_OFFSET(Camera3D, fovy, 36); ASSERT_OFFSET(Camera3D, projection, 40);
ASSERT_SIZE(Texture2D, 20);
ASSERT_OFFSET(Texture2D, id, 0); ASSERT_OFFSET(Texture2D, format, 16);

int jolt_raylib_abi_layout_ok(void) { return 1; }
int jolt_raylib_abi_color(Color value) { return value.r + 10*value.g + 100*value.b + 1000*value.a; }
float jolt_raylib_abi_vector2(Vector2 value) { return value.x + 10.0f*value.y; }
float jolt_raylib_abi_vector3(Vector3 value) { return value.x + 10.0f*value.y + 100.0f*value.z; }
float jolt_raylib_abi_rectangle(Rectangle value) { return value.x + 10.0f*value.y + 100.0f*value.width + 1000.0f*value.height; }
float jolt_raylib_abi_camera2d(Camera2D value) { return value.offset.x + value.offset.y + value.target.x + value.target.y + value.rotation + value.zoom; }
float jolt_raylib_abi_camera3d(Camera3D value) { return value.position.x + value.target.y + value.up.z + value.fovy + (float)value.projection; }
int jolt_raylib_abi_texture(Texture2D value) { return (int)value.id + value.width + value.height + value.mipmaps + value.format; }
Vector2 jolt_raylib_abi_make_vector2(float x, float y) { return (Vector2){x, y}; }
Texture2D jolt_raylib_abi_make_texture(uint32_t id, int width, int height, int mipmaps, int format) {
    return (Texture2D){id, width, height, mipmaps, format};
}
