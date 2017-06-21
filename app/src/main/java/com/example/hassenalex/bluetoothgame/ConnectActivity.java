package com.example.hassenalex.bluetoothgame;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class ConnectActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_DISCOVERABILITY = 2;

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private long timeStart;

    private BluetoothAdapter mBtAdapter;

    Dialog listDialog;

    public static BluetoothService btService;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    ArrayAdapter<String> mArrayAdapter;
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initialize new BluetoothService
        btService = new BluetoothService(this, mHandler);

        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        listDialog = new Dialog(this);
        listDialog.setContentView(R.layout.list_dialog);

        ListView lv = (ListView) listDialog.findViewById(R.id.lv);
        listDialog.setCancelable(true);
        listDialog.setTitle("ListView");
        lv.setAdapter(mArrayAdapter);
        lv.setOnItemClickListener(mDeviceClickListener);

        initBluetooth();

    }


    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                toast("Service started..", getApplicationContext());
                btService.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initBluetooth() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            toast("No Bluetooth adapter available.", getApplicationContext());
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            Button bt_search = (Button)findViewById(R.id.bt_search);
            bt_search.setEnabled(true);
            Button bt_discover = (Button)findViewById(R.id.bt_discoverability);
            bt_discover.setEnabled(true);
        }
    }


    public static void toast (String msg, Context ctx) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                toast("Bluetooth is activated now.", getApplicationContext());
                Button bt_search = (Button)findViewById(R.id.bt_search);
                bt_search.setEnabled(true);
                Button bt_discover = (Button)findViewById(R.id.bt_discoverability);
                bt_discover.setEnabled(true);
            }
            else {
                toast("Bluetooth is not active.", this);
            }
        } else if (requestCode == REQUEST_DISCOVERABILITY) {
            toast("Device visible for the next 3 minutes.", this);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //searches visible devices
    public void searchdevices(View v){
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        mBtAdapter.startDiscovery();
        toast("searching...", this);
        listDialog.show();
    }

    //enable visibility of the device
    public void enablediscoverability(View v){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABILITY);
    }

    //sends a test message to the connected device
    public static void sendtest(View v){
        String message = "Test Message sent";
        byte[] send = message.getBytes();
        btService.write(send);
    }

    // this starts the 'game'
    public void startgame(View v){
        toast("starting the game ..", getApplicationContext());
        Intent i = new Intent(ConnectActivity.this, GameActivity.class);
        String readyflag = "Yes";
        i.putExtra("AREYOUREADY", readyflag);
        startActivity(i);
    }

    //this starts the measurement
    public void measureDelay(View v) {
        timeStart = System.nanoTime();
        write("1");
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            //Intent intent = new Intent();
            //intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            ConnectActivity.this.onListDialogReturn(address);
            listDialog.dismiss();
        }
    };

    private void onListDialogReturn(String address) {
        if (address != null) {
            // Get the BluetoothDevice object
            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

            mConnectedDeviceName = device.getName();
            // Attempt to connect to the device
            connect(device);
        }
    }

    private void connect(BluetoothDevice device) {
        btService.connect(device, true);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        btService.stop();
        super.onDestroy();
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //setStatus("Connected");
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //setStatus("Connecting..");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //setStatus("Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    int num = -1;
                    try {
                        num = Integer.parseInt(readMessage);
                    } catch (NumberFormatException e) {
                        toast(readMessage, getApplicationContext());
                        if (readMessage.compareTo("YOUREADY?") == 0) {
                            toast("starting the game ..", getApplicationContext());
                            Intent i = new Intent(ConnectActivity.this, GameActivity.class);
                            String readyflag = "No";
                            i.putExtra("AREYOUREADY", readyflag);
                            startActivity(i);
                        }
                    }
                    if (num != -1 && num < 1000) {
                        ++num;
                        write("" + num);
                    } else if (num != -1 && num == 1000) {
                        long elapsedNano = System.nanoTime() - timeStart;
                        double elapsedPerMessageMilli = elapsedNano/1000000000.0;
                        toast("" + elapsedPerMessageMilli, ConnectActivity.this);
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    if(mConnectedDeviceName != null){
                        toast("Connected to " + mConnectedDeviceName, getApplicationContext());
                    }
                    break;
            }
        }
    };

    private void write (String msg) {
        byte[] bytes = msg.getBytes();
        btService.write(bytes);
    }

}
