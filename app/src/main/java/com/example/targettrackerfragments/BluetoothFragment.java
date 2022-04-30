package com.example.targettrackerfragments;

import android.Manifest;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothFragment extends Fragment {

    private static final String TAG = "Bluetooth";

    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    private ItemViewModel viewModel;

    BluetoothConnectionService mBluetoothConnection;
    Button btnReboot;
    Button btnConnect;
    Button btnDiscover;
    Button btnStart;
    Button btnStop;

    TextView tvDiscoverInfo;
    TextView tvConnectionInfo;

    //Textview to view outputstream
    TextView incomingMessages;
    List<String> messages;
    String command;
    boolean on = false;


    //static instance of activity
    private static Context context = null;

    private static final UUID MY_UUID_INSECURE = UUID.fromString("87b585d1-84c3-486a-8f3d-77cf16f84f30");

    BluetoothDevice mBTDevice;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);

                if (device.getAddress().equals("DC:A6:32:3D:0B:48")) {
                    tvDiscoverInfo.setText("Target Tracker found, ready to connect!");
                }

                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                //mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                //lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        try {
            getActivity().unregisterReceiver(mBroadcastReceiver1);
            getActivity().unregisterReceiver(mBroadcastReceiver2);
            getActivity().unregisterReceiver(mBroadcastReceiver3);
            getActivity().unregisterReceiver(mBroadcastReceiver4);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    //set view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onStart() {
        super.onStart();

        context = getActivity();

        viewModel = new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe((LifecycleOwner) getActivity(), item -> {

            if (item.equals("photo") || item.equals("start") || item.equals("quit") || item.equals("capture")
             || item.equals("group") || item.equals("refresh") || item.equals("shotimage") || item.contains("login")
             || item.equals("reboot")) {
                if (item.contains("login")) {

                    item = item.replace("login/", "login:");

                    try {
                        byte[] bytes = item.getBytes(Charset.defaultCharset());
                        mBluetoothConnection.write(bytes);
                    } catch(NullPointerException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        byte[] bytes = item.getBytes(Charset.defaultCharset());
                        mBluetoothConnection.write(bytes);
                    } catch(NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }


        });


        Button btnEnableBT = (Button) getView().findViewById(R.id.btnEnableBT);
        btnEnableDisable_Discoverable = (Button) getView().findViewById(R.id.btnDiscoverable_on_off);
        mBTDevices = new ArrayList<>();

        btnConnect = (Button) getView().findViewById(R.id.btnConnect);
        btnDiscover = (Button) getView().findViewById(R.id.btnFindUnpairedDevices);
        btnStart = (Button) getView().findViewById(R.id.btnStart);
        btnStop = (Button) getView().findViewById(R.id.btnStop);
        btnReboot = (Button) getView().findViewById(R.id.btnReboot);

        tvDiscoverInfo = (TextView) getView().findViewById(R.id.tvDiscoverInfo);
        tvConnectionInfo = (TextView) getView().findViewById(R.id.tvConnectionInfo);

        messages = new ArrayList<String>();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnEnableBT.setOnClickListener(view -> enableDisableBT());

        btnConnect.setOnClickListener(view -> onItemClick());

        btnDiscover.setOnClickListener(view -> Discover());

        btnStart.setOnClickListener(view -> onStartClick());
        btnStop.setOnClickListener(view -> onStopClick());

        btnReboot.setOnClickListener(view -> onRebootClick());

        btnEnableDisable_Discoverable.setOnClickListener(view -> btnEnableDisable_Discoverable());

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");

            messages.add(text);
            viewModel.setData(text);
        }
    };

    //***remember the connection will fail and app will crash if you haven't paired first
    public void startConnection(){

        for (int i = 0; i < mBTDevices.size(); i++) {
            if (mBTDevices.get(i).getAddress().equals("DC:A6:32:3D:0B:48")) {
                mBTDevice = mBTDevices.get(i);
                startBTConnection(mBTDevice, MY_UUID_INSECURE);
                break;
            }

            if (i == mBTDevices.size() - 1) {
                Log.d(TAG, "startConnection: Target Tracker not found");
            }
        }
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOMM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void Discover() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        tvDiscoverInfo.setText("Looking for Target Tracker...");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have Bluetooth capabilities");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }

    public void btnEnableDisable_Discoverable () {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        getActivity().registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = getActivity().checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += getActivity().checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    public void onItemClick() {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();
        tvConnectionInfo.setText("Device not found, please try to discover again or restart Tracker.");

        String deviceName = "", deviceAddress = "";

        for (int j = 0; j < mBTDevices.size(); j++) {
            if ((mBTDevices.get(j).getAddress().equals("DC:A6:32:3D:0B:48"))){
                //create the bond.
                //NOTE: Requires API 17+? I think this is JellyBean

                tvConnectionInfo.setText("");

                deviceName = mBTDevices.get(j).getName();
                deviceAddress = mBTDevices.get(j).getAddress();

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                    Log.d(TAG, "Trying to pair with " + deviceName);
                    mBTDevices.get(j).createBond();

                    mBTDevice = mBTDevices.get(j);
                    mBluetoothConnection = new BluetoothConnectionService(BluetoothFragment.context);
                }
                startConnection();
                break;
            }
        }

        if (!deviceAddress.equals("DC:A6:32:3D:0B:48")) {
            tvConnectionInfo.setText("Device not found, please try to discover again or restart Tracker.");
        }
    }

    //start and stop
    public void onStartClick() {

            command = "start";
            viewModel.setData(command);

    }

    public void onStopClick() {

        command = "quit";
        viewModel.setData(command);

    }

    public void onRebootClick() {
        command = "reboot";
        viewModel.setData(command);

    }
}