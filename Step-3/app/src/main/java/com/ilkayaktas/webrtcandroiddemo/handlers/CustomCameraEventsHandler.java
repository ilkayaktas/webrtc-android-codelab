package com.ilkayaktas.webrtcandroiddemo.handlers;

import android.util.Log;
import org.webrtc.CameraVideoCapturer;

public class CustomCameraEventsHandler implements CameraVideoCapturer.CameraEventsHandler {

    private String logTag = this.getClass().getCanonicalName();

    @Override
    public void onCameraError(String s) {
        Log.d(logTag, "onCameraError() called with: s = [" + s + "]");
    }

    @Override
    public void onCameraDisconnected() {
        Log.d(logTag, "onCameraDisconnected() called ");
    }

    @Override
    public void onCameraFreezed(String s) {
        Log.d(logTag, "onCameraFreezed() called with: s = [" + s + "]");
    }

    @Override
    public void onCameraOpening(String s) {
        Log.d(logTag, "onCameraOpening() called with: i = [" + s + "]");
    }

    @Override
    public void onFirstFrameAvailable() {
        Log.d(logTag, "onFirstFrameAvailable() called");
    }

    @Override
    public void onCameraClosed() {
        Log.d(logTag, "onCameraClosed() called");
    }
}
