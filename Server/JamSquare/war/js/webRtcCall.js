// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.

var signalingChannel;
var id;
var pc;

// TODO(houssainy) uncomment this method
// $(document).ready(function() {
//     connectToSignallingServer('chromeClient');
// });

function connectToSignallingServer(callBack) {
  if(!id)
    id = document.getElementById('id').value;
	$.get('/connect?id=' + id, function(data) {
		channel = new goog.appengine.Channel(data);

		signalingChannel = channel.open();

    // TODO
    signalingChannel.onopen = onOpen.bind(this, callBack);
    signalingChannel.onmessage = onMessage;
    signalingChannel.onerror = onError;
    signalingChannel.onclose = onClose;

    signalingChannel.send = send;
	});
};

function onOpen (callBack) {
  if(callBack)
    callBack();
  console.log('Connection opened...');
  document.getElementById('status').innerHTML = 'Connected'
};

function send(data) {
  $.post("/eventupdate", data, function(resp) {
    console.log("Response: " + resp);
  });
}

function onMessage (signal) {
  if (!pc)
    createPeerConnection();

  msg = JSON.parse(signal.data);
  if (msg.type == "offer") {
    pc.setRemoteDescription(new RTCSessionDescription(msg.data));
    pc.createAnswer(gotAnswer);
  } else if (msg.type == "answer") {
    pc.setRemoteDescription(new RTCSessionDescription(msg.data));
  } else if(msg.type == "candidate") { // Candidate
    pc.addIceCandidate(new RTCIceCandidate(msg.data));
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

	// Ask some server to give us TURN server credentials and URIs.
	function createTurnConfig(onSuccess, onError) {
		// TODO(houssainy) implement this method to get TURN surver for relaying
	};
};

// TODO(houssainy) removce this test method
function createTestOffer () {
  id = '__quadcopter1992';
  connectToSignallingServer(function() {
    createPeerConnection();

    navigator.webkitGetUserMedia({video:true, audio:true},
        onUserMediaSuccess, failed);

    function onUserMediaSuccess(stream) {
      console.log("User has granted access to local media.");

      pc.addStream(stream);

      var remoteView = document.getElementById("remoteView");
      remoteView.src = URL.createObjectURL(stream);

      pc.createOffer(gotOffer, failed);

      function gotOffer(offer) {
        console.log("Offer Created");
        pc.setLocalDescription(offer, onSetSessionDescriptionSuccess, failed);

        var sdpData = {
          "type" : "offer",
          "id" : id,
          "data" : offer
        };
        signalingChannel.send(JSON.stringify(sdpData));
      }

      function onSetSessionDescriptionSuccess() {
        console.log("Set session description success.");
      }
    }

    function failed() {
      console.log('Failed!');
    }
  });
}