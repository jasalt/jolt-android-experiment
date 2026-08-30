cmake_minimum_required(VERSION 3.22.1)
project(jolt_raylib_loop C)

if(NOT DEFINED ENV{RAYLIB_SOURCE})
  message(FATAL_ERROR "RAYLIB_SOURCE must name the flake-pinned Raylib source")
endif()

set(RAYLIB_SOURCE "$ENV{RAYLIB_SOURCE}")
list(APPEND CMAKE_MODULE_PATH "${RAYLIB_SOURCE}/cmake")
add_subdirectory("${RAYLIB_SOURCE}/src" raylib-build EXCLUDE_FROM_ALL)

add_library(main SHARED main.c voxel_sensor.c voxel_orientation.c "${CMAKE_CURRENT_LIST_DIR}/../../../../abi/aggregate_oracle.c")
target_include_directories(main PRIVATE "${RAYLIB_SOURCE}/src"
                                         "${CMAKE_ANDROID_NDK}/sources/android/native_app_glue")

# Optional Voxel Siege probe. Keep Box3D outside the default gallery build; the
# reproducible probe supplies BOX3D_SOURCE pointing at the pinned checkout.
if(DEFINED ENV{BOX3D_SOURCE} AND DEFINED ENV{VOXEL_SOURCE})
  set(BOX3D_SOURCE "$ENV{BOX3D_SOURCE}")
  set(VOXEL_SOURCE "$ENV{VOXEL_SOURCE}")
  file(GLOB BOX3D_SOURCES CONFIGURE_DEPENDS "${BOX3D_SOURCE}/src/*.c")
  add_library(box3d STATIC ${BOX3D_SOURCES})
  target_include_directories(box3d PRIVATE "${BOX3D_SOURCE}/include"
                                           "${BOX3D_SOURCE}/src")
  target_sources(main PRIVATE "${VOXEL_SOURCE}/native/voxel_b3.c")
  target_include_directories(main PRIVATE "${BOX3D_SOURCE}/include"
                                          "${BOX3D_SOURCE}/src")
  target_compile_definitions(main PRIVATE VOXEL_BOX3D_PROBE=1)
  target_link_libraries(main PRIVATE box3d)
endif()

if(DEFINED ENV{VOXEL_ORIENTATION_PROBE})
  target_compile_definitions(main PRIVATE VOXEL_ORIENTATION_PROBE=1)
endif()

if(DEFINED ENV{VOXEL_ASSET_PROBE})
  target_compile_definitions(main PRIVATE VOXEL_ASSET_PROBE=1)
endif()

target_link_options(main PRIVATE "-u" "ANativeActivity_onCreate"
                                  "-Wl,--wrap=fopen")
target_link_libraries(main PRIVATE raylib log android EGL GLESv2 OpenSLES atomic c dl)
