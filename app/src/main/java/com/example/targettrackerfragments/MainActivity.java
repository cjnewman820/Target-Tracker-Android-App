package com.example.targettrackerfragments;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //user clicks start tracking button
    public void onTargetClick (View view) {
        Intent intent = new Intent(this, Tracking.class);
        startActivity(intent);
    }


}