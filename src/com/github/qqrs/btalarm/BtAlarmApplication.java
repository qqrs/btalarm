package com.github.qqrs.btalarm;

import android.app.Application;


public class BtAlarmApplication extends Application {
	
	private AlarmReceiver mReceiver;
	private BluetoothChatService mService;
	
	public AlarmReceiver getAlarmReceiver() {
		
		if (mReceiver == null) {
			mReceiver = new AlarmReceiver();
		}
		
		return mReceiver;
	}
	
	public BluetoothChatService getBluetoothChatService() {
		
		if (mService == null) {
			mService = new BluetoothChatService(this);
		}
		
		return mService;
	}

}
