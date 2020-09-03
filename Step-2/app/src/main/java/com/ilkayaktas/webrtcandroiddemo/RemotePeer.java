package com.ilkayaktas.webrtcandroiddemo;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityRecord;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class RemotePeer implements IceEvents {
    private static final String TAG = "RemotePeer";

    public PeerConnection peerConnection;
    public TCPConnectionClient tcpConnectionClient;
    public VideoRenderer remoteRenderer;
    public SurfaceViewRenderer remoteVideoView;
    public Activity activity;

    public RemotePeer(PeerConnection peerConnection, TCPConnectionClient tcpConnectionClient) {
        this.peerConnection = peerConnection;
        this.tcpConnectionClient = tcpConnectionClient;
    }

    public RemotePeer(PeerConnectionFactory peerConnectionFactory,
                      TCPConnectionClient.TCPConnectionEvents tcpConnectionEvents,
                      String ip, int port,
                      SurfaceViewRenderer remoteVideoView,
                      Activity activity) {

        this.peerConnection = peerConnectionFactory.createPeerConnection(new ArrayList<>(),
                new CustomPeerConnectionObserver("LOCAL_PEER_CREATION") {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        super.onIceCandidate(iceCandidate);
                        onIceCandidateReceived(peerConnection, iceCandidate);
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


    @Override
    public void onIceCandidateReceived(PeerConnection peerConnection, IceCandidate iceCandidate) {

        //we have received ice candidate. We can set it to the other peer.
        Log.d(TAG, "CANDIDATE OLUŞTU.");

        JSONObject object = new JSONObject();
        try {
            object.put("messageType", "ice");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);

            //tcpConnectionClient.send(object.toString());

            /*if (!iAmCaller){
                tcpConnectionServer.send(object.toString());
            } else{
                peer.getRemoteDescription();
            }
            for (int i = 0; i < 3; i++){
                if (!myIP().equals(ipList[i])){
                    tcpConnectionClient[i].send(object.toString());
                }
            }*/

            tcpConnectionClient.send(object.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        peerConnection.addIceCandidate(iceCandidate);
    }

    @Override
    public void gotRemoteStream(MediaStream stream) {

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
