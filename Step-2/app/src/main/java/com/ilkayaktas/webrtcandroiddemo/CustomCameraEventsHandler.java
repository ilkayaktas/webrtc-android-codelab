package com.ilkayaktas.webrtcandroiddemo;

import android.util.Log;

import org.webrtc.CameraVideoCapturer;

public class CustomCameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

    private String TAG = "XXXX CustomCameraEventsHandler";


    @Override
    public void onCameraError(String s) {
        Log.d(TAG, "onCameraError() called with: s = [" + s + "]");
    }

    @Override
    public void onCameraDisconnected() {
        Log.d(TAG, "onCameraDisconnected() called ");
    }

    @Override
    public void onCameraFreezed(String s) {
        Log.d(TAG, "onCameraFreezed() called with: s = [" + s + "]");
    }

    @Override
    public void onCameraOpening(String s) {
        Log.d(TAG, "onCameraOpening() called with: i = [" + s + "]");
    }

    @Override
    public void onFirstFrameAvailable() {
        Log.d(TAG, "onFirstFrameAvailable() called");
    }

    @Override
    public void onCameraClosed() {
        Log.d(TAG, "onCameraClosed() called");
    }
}
