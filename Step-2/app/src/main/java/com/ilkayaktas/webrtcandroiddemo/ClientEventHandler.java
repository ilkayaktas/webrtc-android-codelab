package com.ilkayaktas.webrtcandroiddemo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.SessionDescription;

public class ClientEventHandler implements TCPConnectionClient.TCPConnectionEvents{

    private static final String TAG = "ClientEventHandler";
    MainActivity activity;

    ClientEventHandler(MainActivity activity){
        this.activity = activity;
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

                                for (int i = 0; i < 3; i++){
                                    if (!activity.myIP().equals(activity.ipList[i])){
                                        activity.tcpConnectionClient[i].send(str);
                                    }
                                }

                                //activity.tcpConnectionClient.send(str);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },new MediaConstraints());
                }else if (sdpType.equals("answer")){
                    Log.d(TAG, "ANSWER GELDİ");

                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, payload);

                    activity.localPeerConnection.setRemoteDescription(new CustomSdpObserver("remoteAnswer"), sessionDescription);

                    Log.d(TAG, "tamamlandı");
                }

            }else if (type.equals("ice")){

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
