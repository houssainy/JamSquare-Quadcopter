package com.alexu.csed.jamsquare.pid;

public class PID {
	// PID Parameters.
	double kp; // * (P)ropotional Tuning Parameter
	double ki; // * (I)ntegral Tuning Parameter
	double kd; // * (D)erivative Tuning Parameter

	// the nxt variables are for storing the current states for PID
	private double pidInput;
	private double pidOutput;
	private double pidSetpoint;
	private double iState = 0, lastInput;
	private double outMin, outMax;

	public void updatePID(double input, double setpoint,
			double kp, double ki, double kd) {
		pidInput = input;
		pidSetpoint = setpoint;
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;

	}

	/*
	 * Compute()
	 * **********************************************************************
	 * This, as they say, is where the magic happens. this function should be
	 * called every time "void loop() (activity onCreat())" executes. the
	 * function will decide for itself whether a new pid Output needs to be
	 * computed. returns true when the output is computed, false when nothing
	 * has been done.
	 * ***************************************************************
	 */
	public void Compute() {

		/* Compute all the working error variables */
		double error = pidSetpoint - pidInput;
		iState += error;
		if (iState> outMax)
			iState = outMax;
		else if (iState< outMin)
			iState = outMin;
		double iTerm = (ki * iState);
		double dInput = (pidInput - lastInput);
		/* Compute PID Output */
		pidOutput = kp * error + iTerm - kd * dInput;
		if (pidOutput > outMax)
			pidOutput = outMax;
		else if (pidOutput < outMin)
			pidOutput = outMin;
		// Remember some variables for next time.
		lastInput = pidInput;

	}

	/*
	 * setOutputLimits(...)****************************************************
	 * This function will be used far more often than setInputLimits. while the
	 * input to the controller will generally be in the 0-1023 range (which is
	 * the default already,) the output will be a little different. maybe
	 * they'll be doing a time window and will need 0-8000 or something. or
	 * maybe they'll want to clamp it from 0-125. who knows. at any rate, that
	 * can all be done here.
	 * ************************************************************************
	 */
	public void setOutputLimits(double min, double max) {
		if (min >= max)
			return;
		outMin = min;
		outMax = max;

		if (pidOutput > outMax)
			pidOutput = outMax;
		else if (pidOutput < outMin)
			pidOutput = outMin;

		if (iState > outMax)
			iState = outMax;
		else if (iState < outMin)
			iState = outMin;

	}

	public double getOutput() {
		return pidOutput;
	}

}
