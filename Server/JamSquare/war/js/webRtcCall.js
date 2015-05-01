// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.
//
var signalingChannel;
var dataChannel;
var id;
var pc;

$(document).ready(function() {
  connectToSignallingServer('chromeClient');
});

function connectToSignallingServer(clientId) {
	id = clientId
	$.get('/connect?id=' + id, function(data) {
    console.log('Response = ' + data)
    var jsonMsg = JSON.parse(data)

    if(jsonMsg.type == "error") {
      alert(jsonMsg.data)
      return;
    } else if (jsonMsg.type == "token") {
      var key = jsonMsg.data
      channel = new goog.appengine.Channel(key);

      signalingChannel = channel.open();

      signalingChannel.onopen = onOpen;
      signalingChannel.onmessage = onMessage;
      signalingChannel.onerror = onError;
      signalingChannel.onclose = onClose;

      signalingChannel.send = send;
    }
	});
};

function onOpen() {
  console.log('Connection opened...');
  document.getElementById('status').innerHTML = 'Connected'
};

function send(data) {
  $.post("/eventupdate", data, function(resp) {
    console.log("Response: " + resp);
    var jsonMsg = JSON.parse(data)

    if(jsonMsg.type == "error") {
      alert(jsonMsg.data)
      return;
    }
  });
}

function onMessage (signal) {
  console.log('On message ' + signal)
  if (!pc)
    createPeerConnection();

  var msg = JSON.parse(signal.data);
  if (msg.type == "offer") {
    pc.setRemoteDescription(new RTCSessionDescription(msg.data));
    pc.createAnswer(gotAnswer);
  } else if (msg.type == "answer") {
    pc.setRemoteDescription(new RTCSessionDescription(msg.data));
  } else if(msg.type == "candidate") { // Candidate
    pc.addIceCandidate(new RTCIceCandidate(msg.data));
  } else if(msg.type == "bye") {
	  pc.close();
    document.getElementById('status').innerHTML = 'Not Connected'
	  console.log("Peer Connection Closed.");
  }
  
  function gotAnswer(desc) {
    console.log("Answer Created.");
    pc.setLocalDescription(desc);

    var sdpData = {
      "type" : "answer",
      "id" : id,
      "data" : desc
    };
    signalingChannel.send(JSON.stringify(sdpData));
  };
};

function onError () {
  console.log("Something went wrong!");
};

function onClose () {
  console.log("Connection To Server Closed...");
};

// Create new peerConnection to be used to establish call with other peer.
// Signaling is done through Google App Engine server
// "https://jam-square.appspot.com/".
function createPeerConnection() {
  console.log('Creating peer connection ...');

	pc = new webkitRTCPeerConnection(null, {
		optional : [ {
			RtpDataChannels : true
		} ]
	});

	// send any ice candidates to the other peer
	pc.onicecandidate = function(event) {
    if(event.candidate == null)
      return;
		var candidateData = {
			"type" : "candidate",
      "id" : id,
			"data" : event.candidate
		};
		signalingChannel.send(JSON.stringify(candidateData));
	};

	// once remote stream arrives, show it in the remote video element
	pc.onaddstream = function(event) {
    console.log('New Stream!');
		var remoteView = document.getElementById("remoteView");
		remoteView.src = URL.createObjectURL(event.stream);
	};

  // On Remote data channel oppended.
  pc.ondatachannel = function(event) {
    // TODO(houssainy) Check the label
    console.log("onDataChannel");
    dataChannel = event.channel;

    dataChannel.onopen = function(event) {
      var readyState = dataChannel.readyState;
      if (readyState == "open")
        console.log('Data Channel oppended');
    };

    dataChannel.onmessage = function(event2) {
      console.log("DataChannel Message Received = " + event2.data);
    }
  }

	// Ask some server to give us TURN server credentials and URIs.
	function createTurnConfig(onSuccess, onError) {
		// TODO(houssainy) implement this method to get TURN surver for relaying
	};
};

function sendChannelMessage(msg) {
  if(dataChannel) {
    dataChannel.send(msg);
    console.log(msg + " Sent.");
  } else {
    console.log("Data Channel not created!");
  }
}

window.onclose = function() {
  // TODO(houssainy) Make sure that this function is working!
  if(!pc)
	return;
  var msg = {
      "type" : "bye",
      "id" : id,
    };
  $.post("/eventupdate", JSON.stringify(msg), function(resp) {
    pc.close()
    console.log('Peer Connection Closed');
  });
}
