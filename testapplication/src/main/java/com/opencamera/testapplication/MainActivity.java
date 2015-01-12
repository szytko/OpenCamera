package com.opencamera.testapplication;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.cameracontroller.CameraProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class MainActivity extends Activity {


    protected Button btnCameraStart;
    protected Button btnFlashlightToggle;
    protected boolean flashlightStatus = false;
    protected Camera.Parameters parameters;

    protected SurfaceView preview;
    protected SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.btnCameraStart = (Button)findViewById(R.id.btnCameraStart);
        this.btnFlashlightToggle = (Button)findViewById(R.id.btnFlashlightToggle);

        this.preview = (SurfaceView) findViewById(R.id.preview);
        this.mHolder = preview.getHolder();

        this.btnCameraStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MainActivity.this, CameraScreenActivity.class);
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, 12);
            }
        });

        this.btnFlashlightToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //toggleFlashlight();
            }
        });

        //CameraProvider.getInstance().getCamera();
        Log.e("MainActivity", " ------ CREATE -------");
    }

    @Override
    protected void onDestroy() {
        CameraProvider.getInstance().release();
        Log.e("MainActivity", " ------ DESTROY -------");

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /*
    protected void toggleFlashlight(){
        if (flashlightStatus == false) { // Off, turn it on
            turnOnFlashLight();
        } else { // On, turn it off
            turnOffFlashLight();
        }
    }



    public void turnOnFlashLight() {
        turnOffFlashLight();

        try {
            CameraProvider.getInstance().getCamera().setPreviewDisplay(mHolder);
        }catch (IOException e){
            return;
        }

        CameraProvider.getInstance().getCamera().startPreview();

        parameters = CameraProvider.getInstance().getCamera().getParameters();
        List<String> modes = parameters.getSupportedFocusModes();
        parameters.set("factory-testno", 1);
        parameters.set("af-lamp", "true");
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        String s = parameters.toString();

        CameraProvider.getInstance().getCamera().setParameters(parameters);
        flashlightStatus = true;
    }

    public void turnOffFlashLight() {
        // Turn off flashlight
            parameters = CameraProvider.getInstance().getCamera().getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                CameraProvider.getInstance().getCamera().setParameters(parameters);
            }

        flashlightStatus = false;
    }
    */
}
