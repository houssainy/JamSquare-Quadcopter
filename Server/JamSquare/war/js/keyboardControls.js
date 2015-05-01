// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//
var throttle, yaw, pitch, roll;

function onKeyPressed(event) {
  // var data = {
  //   "throttle" : throttle,
  //   "yaw" : yaw,
  //   "pitch" : pitch,
  //   "roll" : roll
  // };
  // JSON.stringify(data)


      var x = event.which || event.keyCode;
    document.getElementById("demo").innerHTML = "The Unicode value is: " + x;
}