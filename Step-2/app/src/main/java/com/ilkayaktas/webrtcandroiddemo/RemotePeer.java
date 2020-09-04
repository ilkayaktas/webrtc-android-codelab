package com.ilkayaktas.webrtcandroiddemo;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityRecord;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;

public class RemotePeer implements IceEvents {
    private static final String TAG = "XXXX RemotePeer";

    public PeerConnection peerConnection;
    public TCPConnectionClient tcpConnectionClient;
    public VideoRenderer remoteRenderer;
    public SurfaceViewRenderer remoteVideoView;
    public MainActivity activity;

    public RemotePeer(PeerConnectionFactory peerConnectionFactory,
                      TCPConnectionClient.TCPConnectionEvents tcpConnectionEvents,
                      String ip, int port,
                      SurfaceViewRenderer remoteVideoView,
                      MainActivity activity) {
        
        this.peerConnection = peerConnectionFactory.createPeerConnection(new ArrayList<>(),
                new CustomPeerConnectionObserver() {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        super.onIceCandidate(iceCandidate);
                        onIceCandidateReceived(peerConnection, iceCandidate);
                        Log.d(TAG, "onIceCandidate: girdi");
                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                        super.onAddStream(mediaStream);
                        gotRemoteStream(mediaStream);
                    }
                });

        this.tcpConnectionClient = new TCPConnectionClient(Executors.newSingleThreadExecutor(),
                tcpConnectionEvents,
                TCPConnectionClient.TCPConnectionType.CLIENT,
                ip,
                port);

        this.remoteVideoView = remoteVideoView;
        this.activity = activity;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    @Override
    public void onIceCandidateReceived(PeerConnection peerConnection, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        Log.d(TAG, "CANDIDATE OLUŞTU. "+activity.myIP());

        JSONObject object = new JSONObject();
        try {
            object.put("messageType", "ice");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);
            object.put("ip", activity.myIP());
            tcpConnectionClient.send(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        peerConnection.addIceCandidate(iceCandidate);
    }

    @Override
    public void gotRemoteStream(MediaStream stream) {

        Log.d(TAG, "Stream alındı. "+activity.myIP());
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        AudioTrack audioTrack = stream.audioTracks.get(0);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteRenderer = new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);
                    Log.d(TAG, "run: Video gelmeye başladı");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}

interface IceEvents{
    void onIceCandidateReceived(PeerConnection peerConnection, IceCandidate iceCandidate);
    void gotRemoteStream(MediaStream mediaStream);
}
