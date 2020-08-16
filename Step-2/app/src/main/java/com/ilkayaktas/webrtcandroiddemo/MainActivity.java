package com.ilkayaktas.webrtcandroiddemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;

    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;
    VideoRenderer localRenderer;
    VideoRenderer remoteRenderer;

    PeerConnection localPeer, remotePeer;
    Button start, call, hangup;

    // Peers are equally privileged, equipotent participants in the application.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initVideos();
    }


    private void initViews() {
        start = findViewById(R.id.start_call);
        call = findViewById(R.id.init_call);
        hangup = findViewById(R.id.end_call);
        localVideoView = findViewById(R.id.local_gl_surface_view);
        remoteVideoView = findViewById(R.id.remote_gl_surface_view);

        start.setOnClickListener(this);
        call.setOnClickListener(this);
        hangup.setOnClickListener(this);
    }

    private void initVideos() {
        EglBase rootEglBase = EglBase.create();

        // Init with EglBase
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);

        // Set Z Order
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);

        // Set Mirror
        localVideoView.setMirror(true);
        remoteVideoView.setMirror(true);
    }


    private VideoCapturer createVideoCapturer(CustomCameraEventsHandler customCameraEventsHandler) {
        VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false), customCameraEventsHandler);

        return videoCapturer;
    }

    // Create a VideoCapturer instance which uses the camera of the device
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator, CustomCameraEventsHandler customCameraEventsHandler) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            Logging.d(TAG, "Device name " + deviceName);
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, customCameraEventsHandler);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, customCameraEventsHandler);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_call: {
                start();
                break;
            }
            case R.id.init_call: {
                call();
                break;
            }
            case R.id.end_call: {
                hangup();
                break;
            }
        }
    }


    public void start() {
        start.setEnabled(false);
        call.setEnabled(true);
        //Initialize PeerConnectionFactory globals.
        //Params are context, initAudio,initVideo and videoCodecHwAcceleration
        //PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this).setEnableVideoHwAcceleration(true).createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        //Create a new PeerConnectionFactory instance.
        //PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();


        //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
        VideoCapturer videoCapturerAndroid = createVideoCapturer(new CustomCameraEventsHandler());

        //Create a VideoSource instance
        videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        localVideoTrack = peerConnectionFactory.createVideoTrack("101", videoSource);

        // We now have a VideoTrack which gives the stream of data from the device’s camera.

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("102", audioSource);

        //we will start capturing the video from the camera width,height and fps
        videoCapturerAndroid.startCapture(1000, 1000, 30);

        // SurfaceViewRenderer displays video stream on screen
        //create surface renderer, init it and add the renderer to the track
        //SurfaceViewRenderer videoView = findViewById(R.id.local_gl_surface_view);
        localVideoView.setMirror(true);

        //create a videoRenderer based on SurfaceViewRenderer instance
        localRenderer = new VideoRenderer(localVideoView);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addRenderer(localRenderer);

    }


    private void call() {
        start.setEnabled(false);
        call.setEnabled(false);
        hangup.setEnabled(true);

        //we already have video and audio tracks. Now create peerconnections
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        //create sdpConstraints
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        //creating localPeer
        localPeer = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("LOCAL_PEER_CREATION") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(localPeer, iceCandidate);
            }
        });

        //creating remotePeer
        remotePeer = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("REMOTE_PEER_CREATION") {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(remotePeer, iceCandidate);
            }

            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);

            }
        });

        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("103");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);

        //creating Offer
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer"){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //we have localOffer. Set it as local desc for localpeer and remote desc for remote peer.
                //try to create answer from the remote peer.
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                remotePeer.setRemoteDescription(new CustomSdpObserver("remoteSetRemoteDesc"), sessionDescription);
                remotePeer.createAnswer(new CustomSdpObserver("remoteCreateOffer") {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        //remote answer generated. Now set it as local desc for remote peer and remote desc for local peer.
                        super.onCreateSuccess(sessionDescription);
                        remotePeer.setLocalDescription(new CustomSdpObserver("remoteSetLocalDesc"), sessionDescription);
                        localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemoteDesc"), sessionDescription);

                    }
                },new MediaConstraints());
            }
        },sdpConstraints);
    }

    // Clears off all the PeerConnection instances
    private void hangup() {
        localPeer.close();
        remotePeer.close();
        localPeer = null;
        remotePeer = null;
        start.setEnabled(true);
        call.setEnabled(false);
        hangup.setEnabled(false);
    }

    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        AudioTrack audioTrack = stream.audioTracks.get(0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteRenderer = new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    // Set the Ice candidates received from one peer to another peer.
    public void onIceCandidateReceived(PeerConnection peer, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        if (peer == localPeer) {
            remotePeer.addIceCandidate(iceCandidate);
        } else {
            localPeer.addIceCandidate(iceCandidate);
        }
    }



}
