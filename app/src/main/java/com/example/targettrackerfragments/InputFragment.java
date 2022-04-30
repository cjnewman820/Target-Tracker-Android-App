package com.example.targettrackerfragments;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.targettrackerfragments.BluetoothConnectionService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InputFragment extends Fragment {

    SeekBar sbRange;
    TextView tvEnterRange;
    Button btnStart;
    Button btnAnalyze;
    Button btnCapture;
    ImageView targetImage;
    int range;
    boolean on;
    String command;
    ItemViewModel viewModel;
    List<String> messages = new ArrayList<String>();

    BluetoothConnectionService mBluetoothConnection;

    //set view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onStart() {
        super.onStart();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe((LifecycleOwner) getActivity(), item -> {
            messages.add(item);

            if (item.contains("pend")) {

                int start = 0, end = 0;
                String image = "";

                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).contains("pstart")) {
                        start = i;
                    }

                    if (messages.get(i).contains("pend")) {
                        end = i;
                    }
                }

                for (int i = start; i <= end; i++) {
                    image = image + messages.get(i);

                    //Log.d("Message" + i, messages.get(i));
                }

                image = image.replace("pstart", "");
                image = image.replace("pend", "");

                Log.d("Base64 image", image);

//                viewModel.setData(image);

                byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                targetImage.setImageBitmap(decodedImage);

                messages.clear();
            }
        });

        sbRange = (SeekBar) getView().findViewById(R.id.sbRange);
        sbRange.setMax(35);
        sbRange.setMin(15);

        //buttons
        btnStart = (Button) getView().findViewById(R.id.btnStart);
        btnCapture = (Button) getView().findViewById(R.id.btnCapture);

        tvEnterRange = (TextView) getView().findViewById(R.id.tvEnterRange);
        sbRange.setOnSeekBarChangeListener(seekBarChangeListener);

        targetImage = (ImageView) getView().findViewById(R.id.imageViewTarget);

        range = 25;
        on = false;
        tvEnterRange.setText("Range: " + range + " yd");

        btnCapture.setOnClickListener(view -> onCaptureClick());

    }

    android.widget.SeekBar.OnSeekBarChangeListener seekBarChangeListener = new android.widget.SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int range, boolean fromUser) {
            // updated continuously as the user slides the thumb
            tvEnterRange.setText("Range: " + range + " yd");

            command = "Range: " + range + " yd";
            byte[] bytes = command.getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);

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

    //start and stop
    public void onStartStopClick() {

        if (on == false) {

            //try catch
            command = "Start";
            viewModel.setData(command);
            on = true;
        } else {
            command = "Quit";
            viewModel.setData(command);
            on = false;
        }

    }

    public void onCaptureClick() {
        command = "photo";
        viewModel.setData(command);
    }

}