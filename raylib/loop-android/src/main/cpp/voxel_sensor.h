#ifndef VOXEL_SENSOR_H
#define VOXEL_SENSOR_H

/* Owner-thread-only scalar sensor seam for the optional Voxel Siege aim mode. */
int voxel_sensor_start(void);
void voxel_sensor_stop(void);
int voxel_sensor_poll(float out_quaternion[4], long long *timestamp_ns);

#endif
