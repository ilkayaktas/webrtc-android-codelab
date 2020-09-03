package com.ilkayaktas.webrtcandroiddemo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.SessionDescription;

public class ServerEventHandler implements TCPConnectionClient.TCPConnectionEvents{

    private static final String TAG = "ServerEventHandler";
    MainActivity activity;

    ServerEventHandler(MainActivity activity){
        this.activity =activity;
    }

    @Override
    public void onTCPConnected(boolean server) {
        Log.d(TAG, "Someone Connected");
    }

    @Override
    public void onTCPMessage(String message) {
        try {
            JSONObject receivedMessage = new JSONObject(message);

            String type = receivedMessage.getString("messageType");

            if (type.equals("sdp")){

                Log.d(TAG, "SDP RECEIVED");
                String payload = receivedMessage.getString("payload");
                String sdpType = receivedMessage.getString("sdpType");

                if (sdpType.equals("offer")){

                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, payload);

                    Log.d(TAG, "OFFER RECEIVED");

                    activity.localPeerConnection.setRemoteDescription(new CustomSdpObserver("remoteOffer"), sessionDescription);

                    activity.localPeerConnection.createAnswer(new CustomSdpObserver("localAnswer") {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            super.onCreateSuccess(sessionDescription);
                            activity.localPeerConnection.setLocalDescription(new CustomSdpObserver("localAnswer"), sessionDescription);

                            try {
                                JSONObject object = new JSONObject();
                                object.put("messageType", "sdp");
                                object.put("sdpType", "answer");
                                object.put("payload", sessionDescription.description);

                                String str = object.toString();

                                activity.tcpConnectionServer.send(str);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },new MediaConstraints());
                }

            }
            else if (type.equals("ice")){

                Log.d(TAG, "ICE RECEIVED");
                String id = receivedMessage.getString("id");
                int label = receivedMessage.getInt("label");
                String candidate = receivedMessage.getString("candidate");

                activity.localPeerConnection.addIceCandidate(new IceCandidate(id,label,candidate));
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
    }
}
