package com.ilkayaktas.webrtcandroiddemo.signaling;

import org.json.JSONObject;

public interface SignallingInterface {
    void onRemoteHangUp(String msg);

    void onOfferReceived(JSONObject data);

    void onAnswerReceived(JSONObject data);

    void onIceCandidateReceived(JSONObject data);

    void onTryToStart();

    void onCreatedRoom();

    void onJoinedRoom();

    void onNewPeerJoined();
}
