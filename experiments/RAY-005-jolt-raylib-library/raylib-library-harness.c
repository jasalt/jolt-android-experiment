#include <dlfcn.h>
#include <stdio.h>

/* The export is an ABI-table entry, not an ELF symbol. */
typedef int (*init_fn)(int, char **);
typedef void *(*lookup_fn)(const char *);
typedef void (*shutdown_fn)(void);
typedef int (*noop_fn)(void);

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "usage: %s <host-jolt-library>\n", argv[0]);
    return 2;
  }

  void *library = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
  if (!library) {
    fprintf(stderr, "dlopen: %s\n", dlerror());
    return 1;
  }

  init_fn init = (init_fn)dlsym(library, "jolt_library_init");
  lookup_fn lookup = (lookup_fn)dlsym(library, "jolt_lookup");
  shutdown_fn shutdown = (shutdown_fn)dlsym(library, "jolt_library_shutdown");
  if (!init || !lookup || !shutdown || init(0, NULL) != 0) return 3;

  noop_fn noop = (noop_fn)lookup("raylib_host_noop");
  if (!noop || noop() != 7) return 4;

  shutdown();
  puts("raylib_host_noop lookup/call/shutdown: OK");
  return 0;
}
