package com.example.targettrackerfragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import android.app.Fragment;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Tracking extends FragmentActivity {

    private ItemViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe(this, item -> {



        });

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.flFragment, new BluetoothFragment(), "BluetoothFragment TAG");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    //user clicks login button
    public void onLoginClick (View view) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.flFragment, new LoginFragment(), "BluetoothFragment TAG");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    //user clicks bluetooth button
    public void onBluetoothClick (View view) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.flFragment, new BluetoothFragment(), "BluetoothFragment TAG");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    //user clicks start tracking button
    public void onTrackingClick (View view) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.flFragment, new TrackingFragment(), "BluetoothFragment TAG");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}