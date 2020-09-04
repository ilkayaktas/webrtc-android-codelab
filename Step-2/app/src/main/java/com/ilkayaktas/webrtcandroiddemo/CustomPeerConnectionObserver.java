package com.ilkayaktas.webrtcandroiddemo;

import android.util.Log;

import org.webrtc.*;

class CustomPeerConnectionObserver implements PeerConnection.Observer {

    private String TAG = "XXXX CustomPeerConnectionObserver";

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange() called with: signalingState = [" + signalingState + "]");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "onIceConnectionChange() called with: iceConnectionState = [" + iceConnectionState + "]");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange() called with: b = [" + b + "]");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange() called with: iceGatheringState = [" + iceGatheringState + "]");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate() called with: iceCandidate = [" + iceCandidate + "]");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, "onIceCandidatesRemoved() called with: iceCandidates = [" + iceCandidates + "]");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream() called with: mediaStream = [" + mediaStream + "]");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream() called with: mediaStream = [" + mediaStream + "]");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel() called with: dataChannel = [" + dataChannel + "]");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded() called");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack()");
    }
}
