cmake_minimum_required(VERSION 3.22.1)
project(jolt_raylib_c_probe C)

if(NOT DEFINED ENV{RAYLIB_SOURCE})
  message(FATAL_ERROR "RAYLIB_SOURCE must name the flake-pinned Raylib source")
endif()

set(RAYLIB_SOURCE "$ENV{RAYLIB_SOURCE}")
set(ANDROID_NDK "$ENV{ANDROID_NDK_ROOT}")
# The upstream src/CMakeLists is designed to be consumed from the Raylib
# repository root, where cmake/ is on the module path. Add that path explicitly
# while retaining the pinned source tree as an external subdirectory.
list(APPEND CMAKE_MODULE_PATH "${RAYLIB_SOURCE}/cmake")

add_subdirectory("${RAYLIB_SOURCE}/src" raylib-build EXCLUDE_FROM_ALL)

add_library(main SHARED main.c)
# Raylib's static target includes android_native_app_glue, but its object can be
# discarded before NativeActivity resolves ANativeActivity_onCreate. Force the
# entry point to remain in libmain.so (the same requirement upstream documents).
target_link_options(main PRIVATE "-u" "ANativeActivity_onCreate")
target_link_libraries(main PRIVATE raylib log android EGL GLESv2 OpenSLES atomic c)
