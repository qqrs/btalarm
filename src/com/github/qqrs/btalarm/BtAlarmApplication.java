package com.github.qqrs.btalarm;

import android.app.Application;


public class BtAlarmApplication extends Application {
	
	private AlarmReceiver mReceiver;
	private BluetoothService mService;
	
	public AlarmReceiver getAlarmReceiver() {
		
		if (mReceiver == null) {
			mReceiver = new AlarmReceiver();
		}
		
		return mReceiver;
	}
	
	public BluetoothService getBluetoothChatService() {
		
		if (mService == null) {
			mService = new BluetoothService(this);
		}
		
		return mService;
	}

}
