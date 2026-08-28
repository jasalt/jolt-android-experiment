#include <jni.h>
#include <stdlib.h>
#include <android/log.h>

#include "scheme.h"

static void abnormal_exit(void) {
  __android_log_print(ANDROID_LOG_ERROR, "chez_probe", "Chez abnormal exit");
  abort();
}

JNIEXPORT jint JNICALL
Java_net_joltlang_androidpoc_abiprobe_MainActivity_nativeChezAnswer(
    JNIEnv *environment,
    jclass clazz,
    jstring petite_boot_path,
    jstring scheme_boot_path) {
  (void) clazz;
  const char *petite_path = (*environment)->GetStringUTFChars(
      environment, petite_boot_path, NULL);
  const char *scheme_path = (*environment)->GetStringUTFChars(
      environment, scheme_boot_path, NULL);
  if (petite_path == NULL || scheme_path == NULL) {
    return -1;
  }

  __android_log_print(ANDROID_LOG_INFO, "chez_probe", "Sscheme_init");
  Sscheme_init(abnormal_exit);
  Sset_verbose(1);
  __android_log_print(ANDROID_LOG_INFO, "chez_probe", "register petite: %s", petite_path);
  Sregister_boot_file(petite_path);
  __android_log_print(ANDROID_LOG_INFO, "chez_probe", "register scheme: %s", scheme_path);
  Sregister_boot_file(scheme_path);
  __android_log_print(ANDROID_LOG_INFO, "chez_probe", "Sbuild_heap");
  Sbuild_heap(NULL, NULL);
  __android_log_print(ANDROID_LOG_INFO, "chez_probe", "heap built");

  ptr result = Scall2(Stop_level_value(Sstring_to_symbol("+")),
      Sfixnum(40), Sfixnum(2));

  (*environment)->ReleaseStringUTFChars(environment, petite_boot_path, petite_path);
  (*environment)->ReleaseStringUTFChars(environment, scheme_boot_path, scheme_path);
  return Sfixnum_value(result);
}
