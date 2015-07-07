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

var P_UP = 49; // 1
var P_DOWN = 50; // 2

var I_UP = 51; // 3
var I_DOWN = 52; // 4 

var D_UP = 53; // 5
var D_DOWN = 54; // 6

var stepValue = 5;

var throttle = yaw = pitch = roll = 0;

function onKeyPressed(event) {
  var key = event.which || event.keyCode;
  switch(key) {
    case THROTTLE_UP:
      throttle = handleUp(throttle, 100)
      document.getElementById("throttle").innerHTML = throttle
      break;
    case THROTTLE_DOWN:
      throttle = handleDown(throttle, 0)
      document.getElementById("throttle").innerHTML = throttle
      break;
    case YAW_LEFT:
      yaw = handleDown(yaw, -100)
      document.getElementById("yaw").innerHTML = yaw
      break;
    case YAW_RIGHT:
      yaw = handleUp(yaw, 100)
      document.getElementById("yaw").innerHTML = yaw
      break;
    case PITCH_UP:
      pitch = handleUp(pitch, 100)
      document.getElementById("pitch").innerHTML = pitch
      break;
    case PITCH_DOWN:
      pitch = handleDown(pitch, -100)
      document.getElementById("pitch").innerHTML = pitch
      break;
    case ROLL_LEFT:
      roll = handleDown(roll, -100)
      document.getElementById("roll").innerHTML = roll
      break;
    case ROLL_RIGHT:
      roll = handleUp(roll, 100)
      document.getElementById("roll").innerHTML = roll
      break;
    case P_UP:
      upP();
      break;
    case P_DOWN:
      downP();
      break;
    case I_UP:
      upI();
      break;
    case I_DOWN:
      downI();
      break;
    case D_UP:
      upD();
      break;
    case D_DOWN:
      downD();
      break;
  }

  var msg = JSON.stringify({
    "throttle" : throttle,
    "yaw" : yaw,
    "pitch" : pitch,
    "roll" : roll
  });
  sendChannelMessage(msg)
}

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

function updatePIDValues() {
  var p = document.getElementById('p').value;
  var i = document.getElementById('i').value;
  var d = document.getElementById('d').value;

  console.log(p);
  var msg = JSON.stringify({
    "p" : p,
    "i" : i,
    "d" : d,
  });
  sendChannelMessage(msg)
}

function upP() {
  var p = parseFloat(document.getElementById('p').value);
  p = p + 0.1
  document.getElementById('p').value = p
  updatePIDValues()
}

function downP() {
  var p = parseFloat(document.getElementById('p').value);
  p = p - 0.1;
  document.getElementById('p').value = p
  updatePIDValues();
}

function upI() {
  var i = parseFloat(document.getElementById('i').value);
  i = i + 0.001
  document.getElementById('i').value = i
  updatePIDValues()
}

function downI() {
  var i = parseFloat(document.getElementById('i').value);
  i = i - 0.001
  document.getElementById('i').value = i
  updatePIDValues()
}

function upD() {
  var d = parseFloat(document.getElementById('d').value);
  d = d + 0.5
  document.getElementById('d').value = d
  updatePIDValues()
}

function downD() {
  var d = parseFloat(document.getElementById('d').value);
  d = d - 0.5
  document.getElementById('d').value = d
  updatePIDValues()
}    
