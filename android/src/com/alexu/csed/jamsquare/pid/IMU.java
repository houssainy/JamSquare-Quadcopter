package com.alexu.csed.jamsquare.pid;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class IMU implements SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private float[] mGravity;
	private float[] mGeomagnetic;
	private double[] output = { 0, 0, 0 };

	public IMU(Context context) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//		registerSensors();
	}

	public double[] getAngles() {
		return output;
	}

	public void registerSensors() {

		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, magnetometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void unRegisterSensor() {
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

			mGravity = event.values.clone();
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mGeomagnetic = event.values.clone();
		}
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[16];
			float I[] = new float[16];
			float newR[] = new float[16];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X,
					SensorManager.AXIS_Y, newR);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(newR, orientation);

				output[0] =  Math.toDegrees(orientation[1]);
				output[1] =  Math.toDegrees(orientation[2]);
				output[2] = Math.toDegrees(orientation[0]);

			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

}
