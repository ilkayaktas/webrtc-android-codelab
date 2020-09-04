package com.ilkayaktas.webrtcandroiddemo;

import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "XXXX MainActivity";

    RemotePeer remotePeer;

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;

    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remote1VideoView;
    SurfaceViewRenderer remote2VideoView;
    VideoRenderer localRenderer;
    VideoRenderer remoteRenderer;

    //PeerConnection localPeerConnection;
    Button start, call, hangup;

    public String [] ipList = new String[]{"192.168.43.87","192.168.43.74","192.168.43.92"};
    public List<SurfaceViewRenderer> surfaceViewRendererList = new ArrayList<>();

    public Map<String, RemotePeer> clientMap = new HashMap<>();

    public TCPConnectionClient tcpConnectionServer;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initVideos();

        try {
            CompletableFuture.runAsync(() -> {

                tcpConnectionServer = new TCPConnectionClient(Executors.newSingleThreadExecutor(),
                        new ServerEventHandler(this),
                        TCPConnectionClient.TCPConnectionType.SERVER,
                        "0.0.0.0",
                        5555);
            }).get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public String myIP(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
    }

    private void initViews() {
        start = findViewById(R.id.start_call);
        call = findViewById(R.id.init_call);
        hangup = findViewById(R.id.end_call);
        localVideoView = findViewById(R.id.local_gl_surface_view);
        remote1VideoView = findViewById(R.id.remote1_gl_surface_view);
        remote2VideoView = findViewById(R.id.remote2_gl_surface_view);

        surfaceViewRendererList.add(remote1VideoView);
        surfaceViewRendererList.add(remote2VideoView);

        start.setOnClickListener(this);
        call.setOnClickListener(this);
        hangup.setOnClickListener(this);
    }

    private void initVideos() {
        // Android uses the EGL library. GLES calls render textured polygons, while EGL calls put renderings on screens.
        // This is related with OpenGL
        EglBase rootEglBase = EglBase.create();

        // Init with EglBase to have high performance OpenGL drawings
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remote1VideoView.init(rootEglBase.getEglBaseContext(), null);
        remote2VideoView.init(rootEglBase.getEglBaseContext(), null);

        // Set Mirror
        localVideoView.setMirror(true);
        remote1VideoView.setMirror(true);
        remote2VideoView.setMirror(true);
    }

    private VideoCapturer createVideoCapturer(CustomCameraEventsHandler customCameraEventsHandler) {
        VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false) /*Don't capture to text*/, customCameraEventsHandler);
        return videoCapturer;
    }

    @Nullable
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

        //SurfaceViewRenderer displays video stream on screen
        //create surface renderer, init it and add the renderer to the track
        //SurfaceViewRenderer videoView = findViewById(R.id.local_gl_surface_view);
        localVideoView.setMirror(true);

        //create a videoRenderer based on SurfaceViewRenderer instance
        localRenderer = new VideoRenderer(localVideoView);
        //And finally, with our VideoRenderer ready, we
        //can add our renderer to the VideoTrack.
        localVideoTrack.addRenderer(localRenderer);

        //create sdpConstraints
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

        for (int j= 0, i = 0; i < ipList.length ;i++){
            if (!myIP().equals(ipList[i])){
                remotePeer = new RemotePeer(peerConnectionFactory,
                        new ClientEventHandler(this),
                        ipList[i], 5555,
                        surfaceViewRendererList.get(j++),
                        this);

                clientMap.put(ipList[i], remotePeer);
            }
        }
    }


    private void call() {
        start.setEnabled(false);
        call.setEnabled(false);
        hangup.setEnabled(true);

        MediaStream stream = peerConnectionFactory.createLocalMediaStream("103");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);

        for (int i = 0; i < ipList.length ;i++){
            if (!myIP().equals(ipList[i])){
                RemotePeer remotePeer = clientMap.get(ipList[i]);

                PeerConnection peerConnection = remotePeer.getPeerConnection();

                peerConnection.addStream(stream);

                peerConnection.createOffer(new CustomSdpObserver(){
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        //we have localOffer. Set it as local desc for localpeer and remote desc for remote peer.
                        //try to create answer from the remote peer.
                        super.onCreateSuccess(sessionDescription);
                        peerConnection.setLocalDescription(new CustomSdpObserver(), sessionDescription);

                        Log.d(TAG, "onCreateSuccess: OFFER oluştu");

                        try {
                            JSONObject object = new JSONObject();
                            object.put("messageType", "sdp");
                            object.put("sdpType", "offer");
                            object.put("ip", myIP());
                            object.put("payload", sessionDescription.description);

                            String str = object.toString();

                            remotePeer.tcpConnectionClient.send(str);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },sdpConstraints);
            }
        }
    }


    private void hangup() {
    //  localPeerConnection.close();
    //  remotePeerConnection.close();
    //  localPeerConnection = null;
    //  remotePeerConnection = null;
        start.setEnabled(true);
        call.setEnabled(false);
        hangup.setEnabled(false);
        localVideoView.clearImage();
        remote1VideoView.clearImage();
        remote2VideoView.clearImage();
    }

   /*private void gotRemoteStream(MediaStream stream) {
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
                    Log.d(TAG, "run: Video gelmeye başladı");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }*/

   // Set the Ice candidates received from one peer to another peer.
   /* public void onIceCandidateReceived(PeerConnection peer, IceCandidate iceCandidate) {


        //we have received ice candidate. We can set it to the other peer.
        Log.d(TAG, "CANDIDATE OLUŞTU.");

        JSONObject object = new JSONObject();
        try {
            object.put("messageType", "ice");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);

            //tcpConnectionClient.send(object.toString());

            if (!iAmCaller){
                tcpConnectionServer.send(object.toString());
            } else{
                peer.getRemoteDescription();
            }
            for (int i = 0; i < 3; i++){
                if (!myIP().equals(ipList[i])){
                    tcpConnectionClient[i].send(object.toString());
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        localPeerConnection.addIceCandidate(iceCandidate);
    }*/

   /*@Override
        public void onTCPConnected(boolean server) {
          Log.d(TAG, "Someone Connected");
        }

        @Override
        public void onTCPMessage(String message) {

          try {
              JSONObject receivedMessage = new JSONObject(message);

              String type = receivedMessage.getString("messageType");
              String ip = receivedMessage.getString("ip");

              RemotePeer remotePeer = clientMap.get(ip);

              PeerConnection peerConnection = remotePeer.getPeerConnection();

              if (type.equals("sdp")){

                  Log.d(TAG, "SDP RECEIVED");
                  String payload = receivedMessage.getString("payload");
                  String sdpType = receivedMessage.getString("sdpType");

                  if (sdpType.equals("offer")){

                     SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, payload);

                      Log.d(TAG, "OFFER RECEIVED");

                      peerConnection.setRemoteDescription(new CustomSdpObserver("remoteOffer"), sessionDescription);

                      peerConnection.createAnswer(new CustomSdpObserver("localAnswer") {
                          @Override
                          public void onCreateSuccess(SessionDescription sessionDescription) {
                              super.onCreateSuccess(sessionDescription);
                              peerConnection.setLocalDescription(new CustomSdpObserver("localAnswer"), sessionDescription);

                              try {
                                  JSONObject object = new JSONObject();
                                  object.put("messageType", "sdp");
                                  object.put("sdpType", "answer");
                                  object.put("payload", sessionDescription.description);
                                  String str = object.toString();

                                  //tcpConnectionServer.send(str);
                                  //tcpConnectionClient.send(str);

                                  remotePeer.tcpConnectionClient.send(str);

                              } catch (JSONException e) {
                                  e.printStackTrace();
                              }
                          }
                      },new MediaConstraints());
                  }else if (sdpType.equals("answer")){
                     Log.d(TAG, "ANSWER GELDİ");

                      SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, payload);

                      peerConnection.setRemoteDescription(new CustomSdpObserver("remoteAnswer"), sessionDescription);

                      Log.d(TAG, "tamamlandı");
                  }

              }
              else if (type.equals("ice")){

                  Log.d(TAG, "ICE RECEIVED");
                  String id = receivedMessage.getString("id");
                  int label = receivedMessage.getInt("label");
                  String candidate = receivedMessage.getString("candidate");

                  peerConnection.addIceCandidate(new IceCandidate(id,label,candidate));
             }
          } catch (JSONException e) {
              e.printStackTrace();
          }
        }

        @Override
        public void onTCPError(String description) {
          Log.d(TAG, "onTCPError: ");
        }

        @Override
        public void onTCPClose() {
          Log.d(TAG, "onTCPClose: ");
        }*/
}
