package com.ilkayaktas.webrtcandroiddemo.signaling;

import android.annotation.SuppressLint;
import android.util.Log;

import io.socket.client.SocketIOException;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.*;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SignallingClient {
    private static String TAG = "SIGNALLING CLIENT";
    private static SignallingClient instance;
    private String roomName = null;
    private Socket socket;
    boolean isChannelReady = false;
    public boolean isInitiator = false;
    boolean isStarted = false;
    private SignalingInterface callback;

    //This piece of code should not go into production!!
    //This will help in cases where the node server is running in non-https server and you want to ignore the warnings
    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};

    public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();
        }
        if (instance.roomName == null) {
            //set the room name here
            instance.roomName = "ilkay";
        }
        return instance;
    }

    public void init(SignalingInterface signalingInterface) {
        this.callback = signalingInterface;
        try {
            //set the socket.io url here
            socket = IO.socket("http://192.168.43.67:8080");

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "call: EVENT_CONNECT");
                }
            });

            socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "call: EVENT_CONNECT_TIMEOUT");
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "call: EVENT_CONNECT_ERROR"+args);
                }
            });


            if (!roomName.isEmpty()) {
                emitInitStatement(roomName);
            }

            //room created event.
            socket.on("created", args -> {
                Log.d(TAG, "created call() called with: args = [" + Arrays.toString(args) + "]");
                isInitiator = true;
                callback.onCreatedRoom();
            });

            //room is full event
            //socket.on("full", args -> Log.d(TAG, "full call() called with: args = [" + Arrays.toString(args) + "]"));

            //peer joined event
            socket.on("join", args -> {
                Log.d(TAG, "join call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onNewPeerJoined();
            });

            //when you joined a chat room successfully
            socket.on("joined", args -> {
                Log.d(TAG, "joined call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onJoinedRoom();
            });

            //log event
            //socket.on("log", args -> Log.d(TAG, "log call() called with: args = [" + Arrays.toString(args) + "]"));

            //bye event
            //socket.on("bye", args -> callback.onRemoteHangUp((String) args[0]));

            //messages - SDP and ICE candidates are transferred through this
            socket.on("message", args -> {
                Log.d(TAG, "message call() called with: args = [" + Arrays.toString(args) + "]");
                if (args[0] instanceof String) {
                    Log.d(TAG, "String received :: " + args[0]);
                    String data = (String) args[0];
                    if (data.equalsIgnoreCase("got user media")) {
                        callback.onTryToStart();
                    }
                    if (data.equalsIgnoreCase("bye")) {
                        callback.onRemoteHangUp(data);
                    }
                } else if (args[0] instanceof JSONObject) {
                    try {

                        JSONObject data = (JSONObject) args[0];
                        Log.d(TAG, "Json Received :: " + data.toString());
                        String type = data.getString("type");
                        if (type.equalsIgnoreCase("offer")) {
                            callback.onOfferReceived(data);
                        } else if (type.equalsIgnoreCase("answer") && isStarted) {
                            callback.onAnswerReceived(data);
                        } else if (type.equalsIgnoreCase("candidate") && isStarted) {
                            callback.onIceCandidateReceived(data);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });


            socket.connect();

            if (socket.connected()){
                Log.d(TAG, "Socket Connected!");
            }else {
                Log.d(TAG, "Socket NOT Connected!");
            }
        } catch (URISyntaxException e ) {
            e.printStackTrace();
        }
    }

    private void emitInitStatement(String message) {
        Log.d(TAG, "emitInitStatement() called with: event = [" + "create or join" + "], message = [" + message + "]");
        socket.emit("create or join", message);
    }

    public void emitMessage(String message) {
        Log.d(TAG, "emitMessage() called with: message = [" + message + "]");
        socket.emit("message", message);
    }

    public void emitMessage(SessionDescription message) {
        try {
            Log.d(TAG, "emitMessage() called with: message = [" + message + "]");
            JSONObject obj = new JSONObject();
            obj.put("type", message.type.canonicalForm());
            obj.put("sdp", message.description);
            Log.d("emitMessage", obj.toString());
            socket.emit("message", obj);
            Log.d("vivek1794", obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void emitIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject object = new JSONObject();
            object.put("type", "candidate");
            object.put("label", iceCandidate.sdpMLineIndex);
            object.put("id", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);
            socket.emit("message", object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void close() {
        socket.emit("bye", roomName);
        socket.disconnect();
        socket.close();
    }

    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONObject data);

        void onTryToStart();

        void onCreatedRoom();

        void onJoinedRoom();

        void onNewPeerJoined();
    }
}
