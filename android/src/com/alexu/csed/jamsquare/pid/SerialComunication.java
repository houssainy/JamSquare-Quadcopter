package com.alexu.csed.jamsquare.pid;

import java.io.IOException;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class SerialComunication {

	private UsbManager usbManager;
	private UsbSerialDriver device;
	private String TAG = "Serial Comunicator";

	public SerialComunication(Context context) {
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
	}

	public void register() {
		device = UsbSerialProber.acquire(usbManager);
		if (device == null) {
			// there is no device connected!
			Log.d(TAG, "No USB serial device connected.");
		} else {
			try {
				// open the device
				device.open();
				// set the communication speed
				device.setBaudRate(115200); // make sure this matches your
											// device's setting!
			} catch (IOException err) {
				Log.e(TAG, "Error setting up USB device: " + err.getMessage(),
						err);
				try {
					// something failed, so try closing the device
					device.close();
				} catch (IOException err2) {
					// couldn't close, but there's nothing more to do!
				}
				device = null;
				return;
			}
		}

	}

	public void unRegister() {
		if (device != null) {
			try {
				device.close();
			} catch (IOException e) {
				// we couldn't close the device, but there's nothing we can do
				// about it!
			}
			// remove the reference to the device
			device = null;
		}
	}

	public void sendToArduino(String data) {
		byte[] dataToSend = data.getBytes();
		// send the color to the serial device
		if (device != null) {
			try {
				device.write(dataToSend, 500);
			} catch (IOException e) {
				Log.e(TAG, "couldn't write bytes to serial device");
			}
		}
	}

}
