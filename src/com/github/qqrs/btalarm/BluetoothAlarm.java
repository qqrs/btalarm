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
import android.view.View;
import android.widget.RadioButton;
import android.widget.Switch;
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
    public static final String PREFS_KEY_LAST_BLUETOOTH_DEVICE_INFO = "lastBluetoothDeviceInfo";
    public static final String PREFS_KEY_BTALARM_ENABLED = "btalarmEnabled";
    public static final String PREFS_KEY_RING_STYLE = "ringStyle";
    private static final String PREFS_KEY_FIRST_APP_RUN = "firstAppRun";

    // Ring style values
    public static final int RING_STYLE_CONTINUOUS = 0;
    public static final int RING_STYLE_SINGLE_RING = 1;
    public static final int RING_STYLE_REPEATED_RING = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Member object for the services
    private BluetoothService mService = null;
    
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
            // TODO: need to unregisterReceiver somewhere?
        	AlarmReceiver.register(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREFS_KEY_FIRST_APP_RUN, false);
        	editor.putBoolean(PREFS_KEY_BTALARM_ENABLED, true);
        	editor.putInt(PREFS_KEY_RING_STYLE, RING_STYLE_CONTINUOUS);
            editor.commit();
        }

        Switch sw = (Switch)findViewById(R.id.switch_enabled);
        sw.setChecked(prefs.getBoolean(PREFS_KEY_BTALARM_ENABLED, true));

        int ringStyle = prefs.getInt(PREFS_KEY_RING_STYLE, RING_STYLE_CONTINUOUS);
        int activeRadioId;
        switch(ringStyle) {
            case RING_STYLE_CONTINUOUS:
                activeRadioId = R.id.radio_ring_continuous;
                break;
            case RING_STYLE_SINGLE_RING:
                activeRadioId = R.id.radio_ring_single;
                break;
            case RING_STYLE_REPEATED_RING:
                activeRadioId = R.id.radio_ring_repeated;
                break;
            default:
                activeRadioId = R.id.radio_ring_continuous;
                return;
        }
        RadioButton active_radio = (RadioButton)findViewById(activeRadioId);
        active_radio.setChecked(true);

        // TODO: populate Bluetooth device name
        TextView textBtName = (TextView)findViewById(R.id.text_btname);
        String btInfo = prefs.getString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_INFO, null);
        if (btInfo == null) {
            textBtName.setText("no device selected");
        } else {
            textBtName.setText(btInfo);
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
        // Otherwise, setup the session
        } else {
            if (mService == null) { 
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
        if (mService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothService.STATE_NONE) {
              // Start the Bluetooth services
              mService.start();
            }
        }
    }

    private void setupBtAlarm() {
        Log.d(TAG, "setupBtAlarm()");

        // Initialize the BluetoothService to perform bluetooth connections
        BtAlarmApplication app = (BtAlarmApplication) getApplication();
        mService = app.getBluetoothService();
        mService.addHandler(mHandler);

        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            // attempt to autoconnect to last Bluetooth device
        	
        	SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String lastBluetoothDeviceAddress = settings.getString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, null);
            if(D) Log.e(TAG, "lastBluetoothDeviceAddress: " + lastBluetoothDeviceAddress);
            if(lastBluetoothDeviceAddress != null) {
                // TODO: don't connect here? only in debug handler and AlarmReceiver?
                mService.connect(this);
            } else {
                // No saved Bluetooth device -- show the device list
                Intent intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE_INSECURE);
            }
        } 
    }
    
    public void onBtnClicked(View btn) {
        // TODO: autoconnect? add connect button?
        int id = btn.getId();

        switch (id) {
        case R.id.button_debug: 
            //TODO
            //if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            if (false) {
                Toast.makeText(this, "Bluetooth device not connected.", Toast.LENGTH_SHORT).show();
                //if(lastBluetoothDeviceAddress != null) {
                    //mService.connect(this);
                //}
            } else {
                Intent intent = null;
                intent = new Intent(this, BluetoothDebugActivity.class);
                startActivity(intent);
            }
            break;
        case R.id.button_scan:
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (prefs.getBoolean(PREFS_KEY_BTALARM_ENABLED, true)) {
                Intent intent = null;
                intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE_INSECURE);
            } else {
                Toast.makeText(this, "Bluetooth Alarm service is disabled. Can't scan for devices.", Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        if (!checked) {
            return;
        }

        int ringStyle;
        
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_ring_continuous:
                ringStyle = RING_STYLE_CONTINUOUS;
                break;
            case R.id.radio_ring_single:
                ringStyle = RING_STYLE_SINGLE_RING;
                break;
            case R.id.radio_ring_repeated:
                ringStyle = RING_STYLE_REPEATED_RING;
                break;
            default:
                return;
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREFS_KEY_RING_STYLE, ringStyle);
        editor.commit();
    }

    public void onSwitchClicked(View view) {
        if (view.getId() != R.id.switch_enabled) {
            return;
        }

        boolean checked = ((Switch) view).isChecked();

        // TODO: stop/start services
        if (checked) {
        } else {
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_KEY_BTALARM_ENABLED, checked);
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
        
        mService.removeHandler(mHandler);
        mService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services

        if (mService != null) {
            // TODO: is this needed? will msg go out before service is stopped?
            // Exit command mode on RN-41 module
        	RN41Gpio.sendCmd(this, mService, RN41Gpio.CMD_END);

            mService.stop();
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
                // Bluetooth is now enabled, so set up a session
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
        // Get the device MAC address and device info string
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        String info = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_INFO);

        // Store and persist address as last Bluetooth device
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, address);
        editor.putString(PREFS_KEY_LAST_BLUETOOTH_DEVICE_INFO, info);
        editor.commit();

        // Update TextView showing selected device
        TextView textBtName = (TextView)findViewById(R.id.text_btname);
        if (info == null) {
            textBtName.setText("no device selected");
        } else {
            textBtName.setText(info);
        }

        // TODO: don't connect here? only in debug handler and AlarmReceiver?
        mService.connect(this);
    }

}
