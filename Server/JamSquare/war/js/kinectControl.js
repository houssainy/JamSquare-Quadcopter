// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//

var rightXPos, rightYPos, rightZPos;
var leftXPos, leftYPos, leftZPos;

var state = "throttleOn"

$(document).ready(function () {
	rightXPos = document.getElementById("right-XPos");
	rightYPos = document.getElementById("right-YPos");
	rightZPos = document.getElementById("right-ZPos");

	leftXPos = document.getElementById("left-XPos");
	leftYPos = document.getElementById("left-YPos");
	leftZPos = document.getElementById("left-ZPos");
	
	// If you call this method from a client that has not called the connect or
	// disconnect function, this function implicitly calls connect with the default
	// parameters. The returned KinectSensor is already in a connected state; you do
	// not need to call the connect function on this interface.
	var sensor = Kinect.sensor(Kinect.DEFAULT_SENSOR_NAME, 
				function (sensorToConfigure, isConnected) {
					console.log('Sensor is connected state: ' + isConnected);
					if(isConnected) {
						var configuration = { 
							"skeleton" : {
								"enabled": true,
							}
						};
					  
						sensorToConfigure.postConfig( configuration );
					}
				});
	//			
	sensor.addStreamFrameHandler( function(frame) {
		switch (frame.stream) {
			case Kinect.SKELETON_STREAM_NAME:
				for (var i = 0; i < uiAdapter.handPointers.length; ++i) {
					var handPointer = uiAdapter.handPointers[i];
					
					// Left Hand
					if(handPointer.handType == "Left") {
						leftXPos.innerHTML = handPointer.rawX.toFixed(2)
						leftYPos.innerHTML = handPointer.rawY.toFixed(2)
						leftZPos.innerHTML = handPointer.rawZ.toFixed(2)
					} else if (handPointer.handType == "Right") {
						// Right hand
						rightXPos.innerHTML = handPointer.rawX.toFixed(2)
						rightYPos.innerHTML = handPointer.rawY.toFixed(2)
						rightZPos.innerHTML = handPointer.rawZ.toFixed(2)
						
						if(state == "throttleOff"" && handPointer.rawY.toFixed(2) < -0.80) {
						  state = "throttleOn"";
						  var msg = JSON.stringify({
							"throttle" : 20,
							"yaw" : 0,
							"pitch" : 0,
							"roll" : 0
						  });
						  sendChannelMessage(msg);
						} else if(state == "throttleOn""){
							state = "throttleOff"";
						  var msg = JSON.stringify({
							"throttle" : 0,
							"yaw" : 0,
							"pitch" : 0,
							"roll" : 0
						  });
						  sendChannelMessage(msg);
						}
						
					}
				}
				
			break;
		}
	});

	// Create a UI adapter for the hand cursor and Kinect buttons.
	var uiAdapter = KinectUI.createAdapter(sensor);

	// Hook up DOM elements that are annotated with the "kinect-button" class to 
	// allow them to be pressed by Kinect interactions.
	uiAdapter.promoteButtons();

	// Create a HandPointerCursor interface that can display or hide the user's hand cursor.
	//var cursor = uiAdapter.createDefaultCursor();
});