package com.github.qqrs.btalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver
{

	private static final String TAG = "AlarmReceiver";
	
	private Context mActiveAlarmContext = null;
	
	public static void register(Context context)
	{
		// This is a boot or the first time the user opens the app.
		// We need to start listening for the alarms
		IntentFilter filter = new IntentFilter(BluetoothChat.ALARM_ALERT_ACTION);
		filter.addAction(BluetoothChat.ALARM_DISMISS_ACTION);
		filter.addAction(BluetoothChat.ALARM_SNOOZE_ACTION);
		filter.addAction(BluetoothChat.ALARM_DONE_ACTION);

		BtAlarmApplication app = (BtAlarmApplication) context.getApplicationContext();

		context.registerReceiver(app.getAlarmReceiver(), filter);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		Log.d(TAG, "received action: " + action);

		if (action.equals(BluetoothChat.ALARM_ALERT_ACTION))
		{
			turnAlarmOn(context);
		}

		if (action.equals(BluetoothChat.ALARM_DISMISS_ACTION) || action.equals(BluetoothChat.ALARM_SNOOZE_ACTION) || action.equals(BluetoothChat.ALARM_DONE_ACTION))
		{
			turnAlarmOff(context);
		}
	}
	
	private void turnAlarmOn(Context context) {
		
		BtAlarmApplication app = (BtAlarmApplication) context.getApplicationContext();
		BluetoothService service = app.getBluetoothChatService();
		
		mActiveAlarmContext = context;
		
		final int state = service.getState();
		if (state != BluetoothService.STATE_CONNECTED && state != BluetoothService.STATE_CONNECTING) {
			
			SharedPreferences settings = context.getSharedPreferences(BluetoothChat.PREFS_NAME, Context.MODE_PRIVATE);
            String lastBluetoothDeviceAddress = settings.getString(BluetoothChat.PREFS_KEY_LAST_BLUETOOTH_DEVICE_ADDRESS, null);
            
            if(lastBluetoothDeviceAddress != null) {
            	service.addHandler(mHandler);
                service.connect(context);
            }
		
		} else {
			sendAlarmOnCmd(context);
		}
	}
	
	private void turnAlarmOff(Context context) {
		
		BtAlarmApplication app = (BtAlarmApplication) context.getApplicationContext();
		BluetoothService service = app.getBluetoothChatService();
		
		mActiveAlarmContext = null;
		
		final int state = service.getState();
		if (state == BluetoothService.STATE_CONNECTED) {
			
			sendAlarmOffCmd(context);
		}
		
		service.removeHandler(mHandler);
	}
	
	private void sendAlarmOnCmd(Context context) {
		sendMessagesWithDelay(context, 1000, RN41Gpio.CMD_BEGIN, RN41Gpio.CMD_ON);
	}
	
	private void sendAlarmOffCmd(Context context) {
		sendMessagesWithDelay(context, 1000, RN41Gpio.CMD_OFF, RN41Gpio.CMD_END);
	}
	
	private void sendMessagesWithDelay(final Context context, final int delayMs, final int... messages) {
		
		new Thread() {
			@Override
			public void run() {
				
				BtAlarmApplication app = (BtAlarmApplication) context.getApplicationContext();
				BluetoothService service = app.getBluetoothChatService();
			
				for (int i = 0; i < messages.length; i++) {
					
					RN41Gpio.sendCmd(context, service, messages[i]);
					
					if (i != messages.length - 1) {
						try {
							Thread.sleep(delayMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
	}
	
	// The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    
                	if (mActiveAlarmContext != null) {
                		sendAlarmOnCmd(mActiveAlarmContext);
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
            // TODO Retry?
//            case BluetoothService.MESSAGE_CONNECTION_FAILED:
//                break;
            // TODO Retry?
//            case BluetoothService.MESSAGE_CONNECTION_LOST:
//                break;
            }
        }
    };

}
