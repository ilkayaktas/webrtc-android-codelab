package com.ilkayaktas.webrtcandroiddemo.signaling;

import com.ilkayaktas.webrtcandroiddemo.handlers.CustomSdpObserver;
import com.ilkayaktas.webrtcandroiddemo.MainActivity;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class SignallingFacade implements SignallingClient.SignalingInterface{

    private MainActivity activity;

    public SignallingFacade(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onRemoteHangUp(String msg) {
        activity.showToast("Remote Peer hungup");
        activity.runOnUiThread(activity::hangup);
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    @Override
    public void onOfferReceived(final JSONObject data) {
        activity.showToast("Received Offer");
        activity.runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                onTryToStart();
            }

            try {
                activity.localPeerConnection.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.OFFER, data.getString("sdp")));
                activity.doAnswer();
                activity.updateVideoViews(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */
    @Override
    public void onAnswerReceived(JSONObject data) {
        activity.showToast("Received Answer");
        try {
            activity.localPeerConnection.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()), data.getString("sdp")));
            activity.updateVideoViews(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remote IceCandidate received
     */
    @Override
    public void onIceCandidateReceived(JSONObject data) {
        try {
            activity.localPeerConnection.addIceCandidate(new IceCandidate(data.getString("id"), data.getInt("label"), data.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
    @Override
    public void onTryToStart() {
        activity.runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isStarted && activity.localVideoTrack != null && SignallingClient.getInstance().isChannelReady) {
                activity.createPeerConnection();
                SignallingClient.getInstance().isStarted = true;
//                if (SignallingClient.getInstance().isInitiator) {
//                    activity.doCall();
//                }
            }
        });
    }

    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     */
    @Override
    public void onCreatedRoom() {
        activity.showToast("You created the room " + activity.gotUserMedia);
        if (activity.gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media");
        }
    }


    /**
     * SignallingCallback - called when you join the room - you are a participant
     */
    @Override
    public void onJoinedRoom() {
        activity.showToast("You joined the room " + activity.gotUserMedia);
        if (activity.gotUserMedia) {
            SignallingClient.getInstance().emitMessage("got user media");
        }
    }

    @Override
    public void onNewPeerJoined() {
        activity.showToast("Remote Peer Joined");
    }
}
