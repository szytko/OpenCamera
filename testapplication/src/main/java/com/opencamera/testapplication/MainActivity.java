package com.opencamera.testapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.cameracontroller.CameraProvider;

import java.io.File;
import java.io.IOException;


public class MainActivity extends Activity {


    protected Button btnCameraStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.btnCameraStart = (Button)findViewById(R.id.btnCameraStart);
        this.btnCameraStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MainActivity.this, CameraScreenActivity.class);
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, 12);
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
}
