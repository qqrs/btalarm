package com.github.qqrs.btalarm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class AlarmRegisterService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {

		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("AlarmRegisterService", "Service received!");
		AlarmReceiver.register(getApplicationContext());
	}

}
