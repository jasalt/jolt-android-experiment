cmake_minimum_required(VERSION 3.22.1)
project(chez_probe C)

set(CHEZ_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../native/chez/android-arm64")

add_library(chez_probe SHARED chez_probe.c)
target_include_directories(chez_probe PRIVATE "${CHEZ_ROOT}")
target_link_libraries(chez_probe PRIVATE
  "${CHEZ_ROOT}/libkernel.a"
  "${CHEZ_ROOT}/zlib/libz.a"
  "${CHEZ_ROOT}/lz4/lib/liblz4.a"
  m
  dl
  log)
