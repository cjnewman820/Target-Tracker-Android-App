package com.example.targettrackerfragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import android.app.Fragment;


import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.charset.Charset;

public class LoginFragment extends Fragment {

    private ItemViewModel viewModel;

    String command;

    Button btnLogin;

    EditText username;
    EditText password;

    TextView loginInfo;

    //static instance of activity
    private static Context context = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        context = getActivity();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe((LifecycleOwner) getActivity(), item -> {

            if (item.equals("success") || item.equals("failure")) {
                if (item.equals("success")) {
                    loginInfo.setText("Successfully Logged In!");
                } else {
                    loginInfo.setText("Incorrect credentials, please try again.");
                }
            }
        });

        btnLogin = (Button) getView().findViewById(R.id.btnLogin);

        loginInfo = (TextView) getView().findViewById(R.id.tvLoginInfo);

        username = (EditText) getView().findViewById(R.id.etUsername);
        password = (EditText) getView().findViewById(R.id.etPassword);

        btnLogin.setOnClickListener(view -> onLoginClick());

    }

    public void onLoginClick() {
        command = "login/" + username.getText().toString() + "/" + password.getText().toString();
        viewModel.setData(command);

    }
}