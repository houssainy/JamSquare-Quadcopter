package com.alexu.csed.jamsquare.pid;

/*
 * THis remote should take inputs from an activity simulating it 
 * */
public class RemotControl {
	private int throttle = 0;
	private int roll = 0;
	private int pitch = 0;
	private int yaw = 0;

	public RemotControl(int throttle , int yaw , int pitch , int roll ){
		this.throttle = throttle;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		}
 
	// if the developer wanna modify  all variables at the same time
	public void  updateRemot(int throttle , int yaw , int pitch , int roll ){
		this.throttle = throttle;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		}
	
	
	public int getThrottle() {
		return throttle;
	}

	public int getRoll() {
		return roll;
	}

	public int getPitch() {
		return pitch;
	}

	public int getYaw() {
		return yaw;
	}

	
}
