package com.almalence.opencam.cameracontroller;

import android.hardware.Camera;

import java.util.List;

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
                this.setCustomParameters(camera);

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

    private void setCustomParameters(Camera camera){
        Camera.Parameters params = camera.getParameters();
        params.set("af-lamp","on");
        Camera.Size s = getBiggest3x2(params);
        if( s!=null ){
            params.setPictureSize(s.width, s.height);
        }
        camera.setParameters(params);
    }

    private Camera.Size getBiggest3x2(Camera.Parameters params){
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size s = null;
        for (Camera.Size candidateSize : sizes) {
            if( candidateSize.width == (int)(1.5*candidateSize.height) && ( s == null || s.width < candidateSize.width ) ){
                s = candidateSize;
            }
        }
        return s;
    }


}
