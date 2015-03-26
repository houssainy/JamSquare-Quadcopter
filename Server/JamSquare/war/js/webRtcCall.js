// Copyright (c) 2015 Jam^2 project authors. All Rights Reserved.

var signalingChannel = createSignalingChannel();
var pc;

// Create new Signaling Channel "webSocketChannel"
function createSignalingChannel() {
	return new WebSocket("/ClientController");
};

signalingChannel.onopen = function() {
	signalingChannel.send(JSON.stringify({
		"type" : "join",
		"id" : "jamSquare"
	}));
};

signalingChannel.onmessage = function(evt) {
	if (!pc)
		createPeerConnection();

	var signal = JSON.parse(evt.data);
	if (signal.type == "offer")
		createAnswer(signal);
	else if(signal.type == "candidate")
		pc.addIceCandidate(new RTCIceCandidate(signal.candidate));
	
	function createAnswer(signal) {
		pc.setRemoteDescription(new RTCSessionDescription(signal.sdp));
		pc.answer(function(desc) {
			console.log("Offer Created.");
			pc.setLocalDescription(desc);

			var sdpData = {
				"type" : "answer",
				"sdp" : desc
			};
			signalingChannel.send(JSON.stringify(sdpData));
		});
	};
};

//Create new peerConnection to be used to establish call with other peer.
//Signaling is done through Google App Engine server
//"https://jam-square.appspot.com/".
function createPeerConnection() {
	pc = new webkitRTCPeerConnection(null, {
		optional : [ {
			RtpDataChannels : true
		} ]
	});

	// send any ice candidates to the other peer
	pc.onicecandidate = function(event) {
		var candidateData = {
			"type" : "candidate",
			"candidate" : event.candidate
		};
		signalingChannel.send(JSON.stringify(candidateData));
	};

	// once remote stream arrives, show it in the remote video element
	pc.onaddstream = function(event) {
		var remoteView = document.getElementById("remoteView");
		remoteView.src = URL.createObjectURL(event.stream);
	};

	// Ask some server to give us TURN server credentials and URIs.
	function createTurnConfig(onSuccess, onError) {
		// TODO(houssainy) implement this method to get TURN surver for relaying
	};
};
