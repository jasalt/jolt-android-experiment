cmake_minimum_required(VERSION 3.22.1)
project(jolt_raylib_loop C)

if(NOT DEFINED ENV{RAYLIB_SOURCE})
  message(FATAL_ERROR "RAYLIB_SOURCE must name the flake-pinned Raylib source")
endif()

set(RAYLIB_SOURCE "$ENV{RAYLIB_SOURCE}")
list(APPEND CMAKE_MODULE_PATH "${RAYLIB_SOURCE}/cmake")
add_subdirectory("${RAYLIB_SOURCE}/src" raylib-build EXCLUDE_FROM_ALL)

add_library(main SHARED main.c "${CMAKE_CURRENT_LIST_DIR}/../../../../abi/aggregate_oracle.c")
target_include_directories(main PRIVATE "${RAYLIB_SOURCE}/src")
target_link_options(main PRIVATE "-u" "ANativeActivity_onCreate")
target_link_libraries(main PRIVATE raylib log android EGL GLESv2 OpenSLES atomic c dl)
