package com.github.qqrs.btalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class AlarmRingerService extends Service
{
	private static final String TAG = "AlarmRingerService";
	
	private boolean mIsAlarmActive = false;
	
	// onStart is deprecated but onStartCommand does not launch as a foreground process by default 
	@Override
	public void onStart(Intent intent, int startid)
	{
		String action = intent.getAction();
		Log.d(TAG, "received action: " + action);

        // Check if alarm is disabled
        SharedPreferences settings = getSharedPreferences(BluetoothAlarm.PREFS_NAME, Context.MODE_PRIVATE);
        if (settings.getBoolean(BluetoothAlarm.PREFS_KEY_BTALARM_ENABLED, true)) {
			if (action.equals(AlarmReceiver.ALARM_ALERT_ACTION))
			{
				turnAlarmOn();
			}
	
			if (action.equals(AlarmReceiver.ALARM_DISMISS_ACTION) || action.equals(AlarmReceiver.ALARM_SNOOZE_ACTION) || action.equals(AlarmReceiver.ALARM_DONE_ACTION))
			{
				turnAlarmOff();
			}
        }
	}
	
	private void turnAlarmOn() {
		BtAlarmApplication app = (BtAlarmApplication)getApplicationContext();
		BluetoothService service = app.getBluetoothService();
		
		mIsAlarmActive = true;
		
		final int state = service.getState();
		if (state != BluetoothService.STATE_CONNECTED && state != BluetoothService.STATE_CONNECTING) {
			
			SharedPreferences settings = getSharedPreferences(BluetoothAlarm.PREFS_NAME, Context.MODE_PRIVATE);
            String lastBluetoothDeviceAddress = settings.getString(BluetoothAlarm.PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, null);
            
            if(lastBluetoothDeviceAddress != null) {
            	service.addHandler(mHandler);
                service.connect(this);
            }
		} else {
			sendAlarmOnCmd();
		}
	}
	
	private void turnAlarmOff() {
		BtAlarmApplication app = (BtAlarmApplication)getApplicationContext();
		BluetoothService service = app.getBluetoothService();
		
		mIsAlarmActive = false;
		
		final int state = service.getState();
		if (state == BluetoothService.STATE_CONNECTED) {
			sendAlarmOffCmd();
		}
		
		service.removeHandler(mHandler);
	}
	
	private void sendAlarmOnCmd() {
        SharedPreferences prefs = getSharedPreferences(BluetoothAlarm.PREFS_NAME, Context.MODE_PRIVATE);
        int ringStyle = prefs.getInt(BluetoothAlarm.PREFS_KEY_RING_STYLE, BluetoothAlarm.RING_STYLE_CONTINUOUS);
        switch(ringStyle) {
            case BluetoothAlarm.RING_STYLE_CONTINUOUS:
            sendMessagesWithDelay(1000, -1, RN41Gpio.CMD_BEGIN, RN41Gpio.CMD_ON);
                break;
            case BluetoothAlarm.RING_STYLE_SINGLE_RING:
            sendMessagesWithDelay(1000, -1, RN41Gpio.CMD_BEGIN, RN41Gpio.CMD_ON, RN41Gpio.CMD_OFF);
                break;
            case BluetoothAlarm.RING_STYLE_REPEATED_RING:
            sendMessagesWithDelay(1000, 1, RN41Gpio.CMD_BEGIN, RN41Gpio.CMD_ON, RN41Gpio.CMD_OFF, RN41Gpio.CMD_OFF, RN41Gpio.CMD_OFF, RN41Gpio.CMD_OFF);
                break;
            default:
                return;
        }
	}
	
	private void sendAlarmOffCmd() {
		sendMessagesWithDelay(1000, -1, RN41Gpio.CMD_OFF, RN41Gpio.CMD_END);
	}
	
	private void sendMessagesWithDelay(final int delayMs, final int repeatIndex, final int... messages) {
		Thread alarmThread = new Thread() {
			@Override
			public void run() {
				BtAlarmApplication app = (BtAlarmApplication)getApplicationContext();
				BluetoothService service = app.getBluetoothService();
			
				for (int i = 0; i < messages.length; i++) {
					if (this != ((BtAlarmApplication)getApplicationContext()).alarmThread) {
						break;
					}
					
					RN41Gpio.sendCmd(app, service, messages[i]);

                    // repeat, starting at command with specified index -- otherwise only run the sequence once
                    if (i == messages.length -1 && repeatIndex >= 0 && repeatIndex < messages.length) {
                    	i = repeatIndex - 1;
                    }
					
					if (i != messages.length - 1) {
						try {
							Thread.sleep(delayMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
        ((BtAlarmApplication)getApplicationContext()).alarmThread = alarmThread;
        alarmThread.start();
	}
	
	// The Handler that gets information back from the BluetoothService
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    // check if alarm is still active before sending commands
                	if (mIsAlarmActive) {
                		sendAlarmOnCmd();
                	}
                    break;
//                case BluetoothService.STATE_CONNECTING:
//                case BluetoothService.STATE_LISTEN:
//                case BluetoothService.STATE_NONE:
                }
                break;
//            case BluetoothService.MESSAGE_WRITE:
//                break;
//            case BluetoothService.MESSAGE_READ:
//                break;
//            case BluetoothService.MESSAGE_DEVICE_NAME:
//                break;
//            case BluetoothService.MESSAGE_CONNECTION_FAILED:
//                break;
//            case BluetoothService.MESSAGE_CONNECTION_LOST:
//                break;
            }
        }
    };

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        ((BtAlarmApplication)getApplicationContext()).alarmThread = null;
    }

}
