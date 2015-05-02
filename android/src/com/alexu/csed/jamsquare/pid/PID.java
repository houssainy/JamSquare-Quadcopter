package com.alexu.csed.jamsquare.pid;

public class PID {
	// PID Parameters.
	double kp; // * (P)ropotional Tuning Parameter
	double ki; // * (I)ntegral Tuning Parameter
	double kd; // * (D)erivative Tuning Parameter
	// we'll hold on to the tuning parameters in user-entered
	// format for display purposes
	private double dispKp;
	private double dispKi;
	private double dispKd;
	private int controllerDirection;
	// the nxt variables are for storing the current states for PID
	private double pidInput;
	private double pidOutput;
	private double pidSetpoint;
	private long lastTime;
	private double iTerm, lastInput;
	private long sampleTime;
	private double outMin, outMax;
	private boolean inAuto;

	public void updatePID(double input, double output, double setpoint,
			double kp, double ki, double kd, int direction) {
		pidOutput = output;
		pidInput = input;
		pidSetpoint = setpoint;
		if (getMode() != Util.AUTOMATIC)
			inAuto = false;
		else
			inAuto = true;
		// default output limit corresponds to the arduino pwm limits
		// setOutputLimits(1000, 2000);
		// default Controller Sample Time is 0.1 seconds
		sampleTime = 100;
		setControllerDirection(direction);
		setTunings(kp, ki, kd);
		lastTime = System.currentTimeMillis() - sampleTime;
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
	public boolean Compute() {
		if (!inAuto)
			return false;
		long now = System.currentTimeMillis();
		long timeChange = (now - lastTime);
		System.out.println(timeChange + "  " + sampleTime);
		if (timeChange >= sampleTime) {
			/* Compute all the working error variables */
			double error = pidSetpoint - pidInput;
			iTerm += (ki * error);
			if (iTerm > outMax)
				iTerm = outMax;
			else if (iTerm < outMin)
				iTerm = outMin;
			double dInput = (pidInput - lastInput);
			/* Compute PID Output */
			pidOutput = kp * error + iTerm - kd * dInput;
			if (pidOutput > outMax)
				pidOutput = outMax;
			else if (pidOutput < outMin)
				pidOutput = outMin;
			// Remember some variables for next time.
			lastInput = pidInput;
			lastTime = now;
			return true;
		} else {
			return false;
		}
	}

	/*
	 * setTunings(...)***********************************************************
	 * ** This function allows the controller's dynamic performance to be
	 * adjusted. it's called automatically from the constructor, but tunings can
	 * also be adjusted on the fly during normal operation
	 * ************************************************************************
	 */
	public void setTunings(double _kp, double _ki, double _kd) {
		if (_kp < 0 || _ki < 0 || _kd < 0)
			return;
		dispKp = _kp;
		dispKi = _ki;
		dispKd = _kd;
		double sampleTimeInSec = ((double) sampleTime) / 1000;
		kp = _kp;
		ki = _ki * sampleTimeInSec;
		kd = _kd / sampleTimeInSec;
		if (controllerDirection == Util.REVERSE) {
			kp = (0 - kp);
			ki = (0 - ki);
			kd = (0 - kd);
		}
	}

	/*
	 * setSampleTime(...)
	 * ********************************************************* sets the
	 * period, in Milliseconds, at which the calculation is performed.
	 * ****************************************************************
	 */
	public void setSampleTime(int newSampleTime) {
		if (newSampleTime > 0) {
			double ratio = (double) newSampleTime / (double) sampleTime;
			ki *= ratio;
			kd /= ratio;
			sampleTime = (long) newSampleTime;
		}
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
		if (inAuto) {
			if (pidOutput > outMax)
				pidOutput = outMax;
			else if (pidOutput < outMin)
				pidOutput = outMin;

			if (iTerm > outMax)
				iTerm = outMax;
			else if (iTerm < outMin)
				iTerm = outMin;
		}
	}

	/*
	 * SetMode(...)**************************************************************
	 * ** Allows the controller Mode to be set to manual (0) or Automatic
	 * (non-zero) when the transition from manual to auto occurs, the controller
	 * is automatically initialized
	 * **********************************************
	 * ******************************
	 */
	public void SetMode(int mode) {
		boolean newAuto = (mode == Util.AUTOMATIC);
		if (newAuto == !inAuto) { // we just went from manual to auto
			Initialize();
		}
		inAuto = newAuto;
	}

	/*
	 * Initialize()**************************************************************
	 * ** does all the things that need to happen to ensure a bumbles transfer
	 * from manual to automatic mode. We should call it before beginning with
	 * the automatic mode
	 * ********************************************************
	 */
	private void Initialize() {
		iTerm = pidOutput;
		lastInput = pidInput;
		if (iTerm > outMax)
			iTerm = outMax;
		else if (iTerm < outMin)
			iTerm = outMin;
	}

	/*
	 * SetControllerDirection(...)***********************************************
	 * ** The PID will either be connected to a DIRECT acting process (+Output
	 * leads to +Input) or a REVERSE acting process(+Output leads to -Input.) we
	 * need to know which one, because otherwise we may increase the output when
	 * we should be decreasing.This mode depends on the motors calibration
	 * state. This is called from the constructor.
	 * *******************************
	 * *********************************************
	 */
	public void setControllerDirection(int direction) {
		if (inAuto && direction != controllerDirection) {
			kp = (0 - kp);
			ki = (0 - ki);
			kd = (0 - kd);
		}
		controllerDirection = direction;
	}

	/*
	 * Status
	 * Functions*************************************************************
	 * Just because you set the Kp=-1 doesn't mean it actually happened. these
	 * functions query the internal state of the PID. they're here for display
	 * purposes. this are the functions the PID Front-end uses for example
	 * *******
	 * *********************************************************************
	 */
	public double getKp() {
		return dispKp;
	}

	public double getKi() {
		return dispKi;
	}

	public double getKd() {
		return dispKd;
	}

	public int getMode() {
		return inAuto ? Util.AUTOMATIC : Util.MANUAL;
	}

	public int getDirection() {
		return controllerDirection;
	}

	public double getOutput() {
		return pidOutput;
	}

}
