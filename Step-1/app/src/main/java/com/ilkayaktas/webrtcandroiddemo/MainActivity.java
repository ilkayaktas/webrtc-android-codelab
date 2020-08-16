package com.ilkayaktas.webrtcandroiddemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.webrtc.*;
import org.webrtc.VideoRenderer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize PeerConnectionFactory globals.
        //Params are context, initAudio,initVideo and videoCodecHwAcceleration
        //PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this).setEnableVideoHwAcceleration(true).createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        //Create a new PeerConnectionFactory instance.
        //PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();


        //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
        VideoCapturer videoCapturerAndroid = createVideoCapturer();

        //Create a VideoSource instance
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        // We now have a VideoTrack which gives the stream of data from the device’s camera.

        //Create MediaConstraints - Will be useful for specifying video and audio constraints. More on this later!
        MediaConstraints constraints = new MediaConstraints();

        //create an AudioSource instance
        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        //we will start capturing the video from the camera width,height and fps
        videoCapturerAndroid.startCapture(1000, 1000, 30);

        // SurfaceViewRenderer displays video stream on screen
        //create surface renderer, init it and add the renderer to the track
        SurfaceViewRenderer videoView = findViewById(R.id.surface_rendeer);
        videoView.setMirror(true);

        EglBase rootEglBase = EglBase.create();
        videoView.init(rootEglBase.getEglBaseContext(), null);

        //Add the renderer to the video track
        localVideoTrack.addRenderer(new VideoRenderer(videoView));

    }


    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));

        return videoCapturer;
    }

    // Create a VideoCapturer instance which uses the camera of the device
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            Logging.d(TAG, "Device name " + deviceName);
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

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
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}
