// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//

// Constants
// Left controller
var THROTTLE_UP = 119; // w
var THROTTLE_DOWN = 115; // s
var YAW_LEFT = 97; // a
var YAW_RIGHT = 100; // d

// Right controller
var PITCH_UP = 111; // o
var PITCH_DOWN = 108; // l
var ROLL_LEFT = 107; // k
var ROLL_RIGHT = 59; // ;

var stepValue = 20;

var throttle = yaw = pitch = roll = 0;

  /*var msg = JSON.stringify({
    "throttle" : throttle,
    "yaw" : yaw,
    "pitch" : pitch,
    "roll" : roll
  });
  sendChannelMessage(msg)*/

function handleUp(val, max) {
  if(val + stepValue > max) {
    console.log("Maximum Value Reached")
    return val;
  }

  return val + stepValue;
}

function handleDown(val, min) {
  if(val - stepValue < min) {
    console.log("Minimum Value Reached")
    return val;
  }

  return val - stepValue;
}

var rightXPos, rightYPos, rightZPos;
var leftXPos, leftYPos, leftZPos;

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
							"interaction" : {
								"enabled": true,
							},
						 
							"userviewer" : {
								"enabled": true,
								"resolution": "640x480", //320x240, 160x120, 128x96, 80x60
								"userColors": { "engaged": 0xffffffff, "tracked": 0xffffffff },
								"defaultUserColor": 0xffffffff, //RGBA
							},

							"skeleton" : {
								"enabled": true,
							},
						 
							"sensorStatus" : {
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
					if(handPointer.handType == "left") {
						leftXPos.innerHTML = handPointer.rawX.toFixed(2)
						leftYPos.innerHTML = handPointer.rawY.toFixed(2)
						leftZPos.innerHTML = handPointer.rawZ.toFixed(2)
					} else if (handPointer.handType == "right") {
						// Right hand
						rightXPos.innerHTML = handPointer.rawX.toFixed(2)
						rightYPos.innerHTML = handPointer.rawY.toFixed(2)
						rightZPos.innerHTML = handPointer.rawZ.toFixed(2)
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