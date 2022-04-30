package com.example.targettrackerfragments;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;




public class TrackingFragment extends Fragment {

    public ArrayList<String> listData = new ArrayList<>();
    public List<String> shotHistory = new ArrayList<>();

    ArrayAdapter<String> listViewAdapter;

    private static Context context;
    ListView shotView;

    Button btnManual;
    Button btnReset;
    Button btnGroup;
    Button btnViewCamera;

    TextView tvShot;
    TextView tvRange;
    TextView tvInfo;

    SeekBar sbRange;

    int shotCountNum = 0;
    int range = 15;
    String command;
    String shotInfo;
    String shotTime;
    List<String> messages = new ArrayList<String>();

    StringBuilder commandData;

    ItemViewModel viewModel;

    ImageView target;

    String TAG = "TrackTarget";

    //set view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracking, container, false);

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onStart() {
        super.onStart();

        target = (ImageView) getView().findViewById(R.id.imageView);

        btnManual = (Button) getView().findViewById(R.id.btnManual);
        btnReset = (Button) getView().findViewById(R.id.btnReset);
        btnGroup = (Button) getView().findViewById(R.id.btnGroup);
        btnViewCamera = (Button) getView().findViewById(R.id.btnViewCamera);

        tvShot = (TextView) getView().findViewById(R.id.shotCount);
        tvRange = (TextView) getView().findViewById(R.id.tvRange);
        tvInfo = (TextView) getView().findViewById(R.id.tvInfo);

        sbRange = (SeekBar) getView().findViewById(R.id.sbRange);
        sbRange.setMax(35);
        sbRange.setMin(15);

        shotView = (ListView) getView().findViewById(R.id.shotHistory);

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                shotHistory
        );

        viewModel = new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe((LifecycleOwner) getActivity(), item -> {

            messages.add(item);

            if (item.contains("group") && item.contains("time")) {

                try {
                    JSONObject object = new JSONObject(item);

                    shotTime = object.getString("time");
                    String [] temp1 = shotTime.split("T");
                    String [] temp2 = temp1[1].split("\\.");
                    shotTime = temp2[0];

                    shotInfo = "Time: " + shotTime + "\nRange: " + range + "\n" + "x: "
                            + object.getString("x") + " y: " + object.getString("y")
                            + "\nGroup: " + object.getString("group");
                    shotHistory.add(shotInfo);

                    shotView.setAdapter(listViewAdapter);
                    listViewAdapter.notifyDataSetChanged();

                    Log.d("JSON: ", shotInfo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                messages.clear();
            } else if (item.contains("pshotend")) {

                tvInfo.setText("");
                int start = 0, end = 0;
                String image = "";

                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).contains("pshotstart")) {
                        start = i;
                    }

                    if (messages.get(i).contains("pshotend")) {
                        end = i;
                    }
                }

                for (int i = start; i <= end; i++) {
                    image = image + messages.get(i);

                }

                image = image.replace("pshotstart", "");
                image = image.replace("pshotend", "");

                Log.d("Base64 image", image);

//                viewModel.setData(image);

                    byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                    Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    target.setImageBitmap(decodedImage);

                messages.clear();
            } else if (item.contains("pend")) {

                int start = 0, end = 0;
                String image = "";

                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).contains("pstart")) {
                        start = i;
                    }

                    if (messages.get(i).contains("pend")) {
                        end = i;
                        Log.d("end number", String.valueOf(end));
                    }
                }

                for (int i = start; i <= end; i++) {
                    image = image + messages.get(i);
                    Log.d("loops", messages.get(i));

                }

                image = image.replace("pstart", "");
                image = image.replace("pend", "");

                Log.d("Base64 image", image);

//                viewModel.setData(image);

                    byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                    Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    target.setImageBitmap(decodedImage);

                messages.clear();
            } else if (item.contains("No new bullet holes found")) {
                tvInfo.setText(item);
            }



        });

        listData = new ArrayList<String>();
        commandData = new StringBuilder();

        btnGroup.setOnClickListener(view -> onGroupClick());
        btnViewCamera.setOnClickListener(view -> onViewCameraClick());
        btnManual.setOnClickListener(view -> onShotClick());
        btnReset.setOnClickListener(view -> onResetClick());

        sbRange = (SeekBar) getView().findViewById(R.id.sbRange);
        sbRange.setMax(35);
        sbRange.setMin(15);

        sbRange.setOnSeekBarChangeListener(seekBarChangeListener);


    }

    android.widget.SeekBar.OnSeekBarChangeListener seekBarChangeListener = new android.widget.SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int range, boolean fromUser) {
            // updated continuously as the user slides the thumb
            tvRange.setText("Range: " + range + " yd");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            range = sbRange.getProgress();
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");

            commandData.append(text + "\n");
            listData.add(text);
        }
    };


    //Manual shot has been made
    public void onShotClick() {

        command = "capture";
        viewModel.setData(command);

        //Change shot count
        shotCountNum++;

        tvShot.setText("Shot Count: " + shotCountNum);

        command = "shotimage";
        viewModel.setData(command);
    }

    public void onGroupClick() {
        command = "group";
        viewModel.setData(command);
    }

    public void onViewCameraClick() {
        command = "photo";
        viewModel.setData(command);
    }

    public void onResetClick() {
        messages.clear();

        //add command to reset group i.e group = 0

        //reset shots
        shotCountNum = 0;
        tvShot.setText("Shot Count: " + shotCountNum);

        //clear shot history and listView
        shotHistory.clear();
        shotView.setAdapter(null);

        //reset range
        range = 15;

        //clear target image
        target.setImageResource(android.R.color.transparent);


    }
}