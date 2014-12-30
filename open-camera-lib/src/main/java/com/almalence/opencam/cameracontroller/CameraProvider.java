package com.almalence.opencam.cameracontroller;

import android.hardware.Camera;
import android.util.Log;

public class CameraProvider {

    private Camera camera;


    private CameraProvider(){};

    private static class CameraProviderHolder{
        private final static CameraProvider instance = new CameraProvider();
    }

    public static CameraProvider getInstance(){
        return CameraProviderHolder.instance;
    }

    public Camera getCamera(){
        try {
            if (camera == null) {
                camera = Camera.open();
            }
            return camera;
        }catch(RuntimeException e){
            return null;
        }
    }

    public void release(){
        if(camera!=null) {
            camera.release();
            camera = null;
        }
    }

}
