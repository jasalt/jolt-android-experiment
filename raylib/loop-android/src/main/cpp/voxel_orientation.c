#include "voxel_orientation.h"

#include <android/native_activity.h>
#include <android_native_app_glue.h>
#include <jni.h>

/* Raylib owns the NativeActivity and exposes this stable process-local accessor. */
extern struct android_app *GetAndroidApp(void);

int voxel_set_orientation(int landscape) {
  struct android_app *app = GetAndroidApp();
  if (app == NULL || app->activity == NULL) return 0;
  JavaVM *vm = app->activity->vm;
  JNIEnv *env = NULL;
  if (vm == NULL || (*vm)->AttachCurrentThread(vm, &env, NULL) != JNI_OK)
    return 0;
  jclass activity_class = (*env)->GetObjectClass(env, app->activity->clazz);
  jmethodID set_orientation = (*env)->GetMethodID(
      env, activity_class, "setRequestedOrientation", "(I)V");
  if (set_orientation == NULL) return 0;
  (*env)->CallVoidMethod(env, app->activity->clazz, set_orientation,
                         landscape ? 6 /* SENSOR_LANDSCAPE */
                                   : 1 /* PORTRAIT */);
  return 1;
}
