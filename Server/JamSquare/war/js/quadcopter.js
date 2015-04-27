// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//

var id;

$(document).ready(function() {
  Android.onPageReady();
});

function connectToSignallingServer(clientId) {
  console.log('Connecting to signaling server...');

  id = clientId;
  if(!id)
    Android.onSignalingError('Missing Id!');

  $.get('/connect?id=' + id, function(data) {
	  console.log('Response = ' + data)
    var jsonMsg = JSON.parse(data)
    
    if(jsonMsg.type == "error") {
      console.log('Error: ' + jsonMsg.data);
      Android.onSignalingError(jsonMsg.data);
      return;
    } else if (jsonMsg.type == "token") {
      var key = jsonMsg.data
      channel = new goog.appengine.Channel(key);

      console.log('Channel API key =' + key);

      signalingChannel = channel.open();

      signalingChannel.onopen = onOpen;
      signalingChannel.onmessage = onMessage;
      signalingChannel.onerror = onError;
      signalingChannel.onclose = onClose;    		
    }
  });
};

function onOpen () {
  document.getElementById('status').innerHTML = 'Channel API Opnned.'
  Android.onConnectToSignalingServer();
};

function onMessage(msg) {
  console.log('On Message ' + msg);
  Android.onMessage(JSON.stringify(msg));
};

function onError () {
  document.getElementById('status').innerHTML = 'ERROR!'
  Android.onError("Something went wrong!");
};

function onClose () {
  document.getElementById('status').innerHTML = 'Closed.'
  Android.onSignalingChannelClosed();
};

function sendToServer(data) {
  $.post("/eventupdate", data, function(resp) {
    Android.onResponse("Response: " + resp);
    var jsonMsg = JSON.parse(data)

    if(jsonMsg.type == "error") {
      alert(jsonMsg.data)
      return;
    }
  });
}

function close() {
   var msg = {
      "type" : "bye",
      "id" : id,
    };
  $.post("/eventupdate", JSON.stringify(msg), function(resp) {
    console.log('Channel API Closed.')
  });
};