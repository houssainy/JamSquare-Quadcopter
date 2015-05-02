package com.alexu.csed.jamsquare.pid;

public final class Util {
	public final static int AUTOMATIC = 1;
	public final static int MANUAL = 0;
	public final static int DIRECT = 0;
	public final static int REVERSE = 1;
	public final static int SAMPLETIME = 10;
	public final static double MIN = 1000.0;
	public final static double MAX = 2000.0;

	public final static double ROLL_PID_MIN = -200.0;
	public final static double ROLL_PID_MAX = 200.0;
	public final static double PITCH_PID_MIN = -200.0;
	public final static double PITCH_PID_MAX = 200.0;
	public final static double YAW_PID_MIN = -100.0;
	public final static double YAW_PID_MAX = 100.0;
}
