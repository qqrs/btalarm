/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.qqrs.btalarm;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the Bluetooth Alarm configuration options.
 */
public class BluetoothAlarm extends Activity {
    
	// Debugging
    private static final String TAG = "BluetoothAlarm";
    private static final boolean D = true;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Preferences file name
    public static final String PREFS_NAME = "btalarm_prefs";
    public static final String PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS = "lastBluetoothDeviceAddress";
    private static final String PREFS_KEY_FIRST_APP_RUN = "firstAppRun";

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Member object for the chat services
    private BluetoothService mChatService = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);
        
        // Register the alarm receiver if this is the first time the app runs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(PREFS_KEY_FIRST_APP_RUN, true);
        if (isFirstTime) {
        	
        	AlarmReceiver.register(this);
        	prefs.edit().putBoolean(PREFS_KEY_FIRST_APP_RUN, false);
        }

        // If the adapter is null, then Bluetooth is not supported
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupBtAlarm() will then be called during onActivityResult
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) { 
                setupBtAlarm();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupBtAlarm() {
        Log.d(TAG, "setupBtAlarm()");

        // Initialize the BluetoothService to perform bluetooth connections
        BtAlarmApplication app = (BtAlarmApplication) getApplication();
        mChatService = app.getBluetoothService();
        mChatService.addHandler(mHandler);

        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            // attempt to autoconnect to last Bluetooth device
        	
        	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String lastBluetoothDeviceAddress = settings.getString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, null);
            if(D) Log.e(TAG, "lastBluetoothDeviceAddress: " + lastBluetoothDeviceAddress);
            if(lastBluetoothDeviceAddress != null) {
                mChatService.connect(this);
                
            } else {
                // No saved Bluetooth device -- show the device list
                Intent intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE_INSECURE);
            }
        } 
    }
    
    public void onBtnClicked(View btn) {
    	
    }

    private void persistLastBluetoothDeviceAddress(String address) {
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, address);
      editor.commit();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
        
        mChatService.removeHandler(mHandler);
        mChatService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services

        if (mChatService != null) {
            // TODO: is this needed? will msg go out before service is stopped?
            // Exit command mode on RN-41 module
        	RN41Gpio.sendCmd(this, mChatService, RN41Gpio.CMD_END);

            mChatService.stop();
        }
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    break;
                case BluetoothService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case BluetoothService.MESSAGE_WRITE:
                break;
            case BluetoothService.MESSAGE_READ:
                break;
            case BluetoothService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothService.MESSAGE_CONNECTION_FAILED:
                Toast.makeText(getApplicationContext(), "Unable to connect device",
                				Toast.LENGTH_SHORT).show();
                break;
            case BluetoothService.MESSAGE_CONNECTION_LOST:
                Toast.makeText(getApplicationContext(), "Device connection was lost",
                				Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupBtAlarm();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // Store and persist address as last Bluetooth device
        persistLastBluetoothDeviceAddress(address);

        mChatService.connect(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        }
        return false;
    }

}
