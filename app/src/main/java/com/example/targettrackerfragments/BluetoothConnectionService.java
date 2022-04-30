package com.example.targettrackerfragments;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    public static final String TAG = "BluetoothConnectionServ";

    private static final String targetTracker = "Target Tracker";

    //private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("87b585d1-84c3-486a-8f3d-77cf16f84f30");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    //private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context mContext) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = mContext;
        start();
    }

    //Waits for a connection
//    private class AcceptThread extends Thread {
//        //Bluetooth server socket
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            BluetoothServerSocket tmp = null;
//
//            //Create new listening server socket
//            try {
//                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(targetTracker, MY_UUID_INSECURE);
//                Log.d(TAG, "AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
//            } catch(IOException e) {
//                Log.e(TAG, e.getMessage());
//            }
//
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//
//            try {
//
//                Log.d(TAG, "run: RFCOM server socket start.....");
//
//                socket = mmServerSocket.accept();
//
//                Log.d(TAG, "run: RFCOM server socket accepted connection.");
//            } catch (IOException e) {
//                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
//            }
//
//            if (socket != null) {
//                connected(socket, mmDevice);
//            }
//
//            Log.i(TAG, "END mAcceptThread ");
//        }
//
//        public void cancel() {
//            Log.d(TAG, "cancel: Cancelling AcceptThread.");
//
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
//            }
//        }
//    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectionThread: started.");
            mmDevice = device;
            deviceUUID = UUID.fromString("87b585d1-84c3-486a-8f3d-77cf16f84f30");

            BluetoothSocket tmp = null;
            Log.i(TAG, "Run mConnectThread");

            //Get Bluetooth Socket for a connection with a given device
            try {
                Log.e(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID:" + MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch(IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;
        }

        public void run() {

            //Cancel discovery so connection will not slow down
            mBluetoothAdapter.cancelDiscovery();

            //Connect to the Bluetooth Socket
            try {
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectedThread: Could not connect to UUID: " + MY_UUID_INSECURE);
                return;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of mmSocket ConnectThread failed. " + e.getMessage());
            }
        }
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        //Cancel thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

//        if (mInsecureAcceptThread == null) {
//            mInsecureAcceptThread = new AcceptThread();
//            mInsecureAcceptThread.start();
//        }
    }

    /**
     AcceptThread starts and waits for connection
     ConnectThread starts and attempts to make a connection once connection is found
     */

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss progress dialog when connection is successful
            try {
                mProgressDialog.dismiss();
            } catch(NullPointerException e) {
                e.printStackTrace();
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[102400]; //buffer for the stream
            int bytes; //bytes returned from read()

            while(true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);

                } catch (IOException e) {
                    Log.e(TAG, "write: error writing to input stream. " + e.getMessage());
                    break;
                }
            }
        }

        //send data to the remote device
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: error writing to output stream. " + e.getMessage());
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting");

        //Start thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        //temp object
        ConnectedThread r;

        Log.d(TAG, "write: Write called.");
        mConnectedThread.write(out);
    }


}

