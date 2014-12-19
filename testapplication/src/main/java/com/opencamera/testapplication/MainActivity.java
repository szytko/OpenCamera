package com.opencamera.testapplication;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.almalence.opencam.CameraScreenActivity;

import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent takePictureIntent = new Intent(this, CameraScreenActivity.class);
        takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, 12);
    }

}
