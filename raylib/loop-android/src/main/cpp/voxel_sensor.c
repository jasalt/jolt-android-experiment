#include "voxel_sensor.h"

#include <android/looper.h>
#include <android/sensor.h>

static ASensorManager *manager;
static ASensorEventQueue *queue;
static const ASensor *sensor;

int voxel_sensor_start(void) {
  if (queue != NULL) return 1;
  manager = ASensorManager_getInstanceForPackage("net.joltlang.raylibgallery");
  sensor = ASensorManager_getDefaultSensor(manager,
                                           ASENSOR_TYPE_GAME_ROTATION_VECTOR);
  if (sensor == NULL) {
    sensor = ASensorManager_getDefaultSensor(manager,
                                             ASENSOR_TYPE_ROTATION_VECTOR);
  }
  if (sensor == NULL) return 0;

  ALooper *looper = ALooper_forThread();
  if (looper == NULL) looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
  if (looper == NULL) return 0;
  queue = ASensorManager_createEventQueue(manager, looper, ALOOPER_POLL_CALLBACK,
                                          NULL, NULL);
  if (queue == NULL) return 0;
  if (ASensorEventQueue_enableSensor(queue, sensor) < 0 ||
      ASensorEventQueue_setEventRate(queue, sensor, 16667) < 0) {
    ASensorManager_destroyEventQueue(manager, queue);
    queue = NULL;
    return 0;
  }
  return 1;
}

void voxel_sensor_stop(void) {
  if (queue == NULL) return;
  ASensorEventQueue_disableSensor(queue, sensor);
  ASensorManager_destroyEventQueue(manager, queue);
  queue = NULL;
  sensor = NULL;
  manager = NULL;
}

int voxel_sensor_poll(float out_quaternion[4], long long *timestamp_ns) {
  if (queue == NULL || out_quaternion == NULL || timestamp_ns == NULL) return 0;
  ASensorEvent event;
  int count = ASensorEventQueue_getEvents(queue, &event, 1);
  if (count <= 0 ||
      (event.type != ASENSOR_TYPE_GAME_ROTATION_VECTOR &&
       event.type != ASENSOR_TYPE_ROTATION_VECTOR)) return 0;
  out_quaternion[0] = event.data[0];
  out_quaternion[1] = event.data[1];
  out_quaternion[2] = event.data[2];
  out_quaternion[3] = event.data[3];
  *timestamp_ns = event.timestamp;
  return 1;
}
