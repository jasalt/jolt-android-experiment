#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>

typedef int (*init_fn)(int, char **);
typedef void *(*lookup_fn)(const char *);
typedef void (*shutdown_fn)(void);
typedef int (*answer_fn)(void);
typedef int (*allocate_fn)(int);

int main(int argc, char **argv) {
  if (argc != 2) { fprintf(stderr, "usage: %s <host-jolt-library>\n", argv[0]); return 2; }
  void *library = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
  if (!library) { fprintf(stderr, "dlopen: %s\n", dlerror()); return 1; }
  init_fn init = (init_fn)dlsym(library, "jolt_library_init");
  lookup_fn lookup = (lookup_fn)dlsym(library, "jolt_lookup");
  shutdown_fn shutdown = (shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (!init || !lookup || !shutdown || init(0, NULL) != 0) return 1;
  answer_fn answer = (answer_fn)lookup("poc_answer");
  allocate_fn allocate = (allocate_fn)lookup("poc_allocate");
  if (!answer || !allocate || answer() != 42) return 1;
  for (int i = 0; i < 100; ++i) if (allocate(10000) != 10000) return 1;
  shutdown();
  puts("host Jolt library lifecycle, lookup, allocation, and shutdown: OK");
  return 0;
}
