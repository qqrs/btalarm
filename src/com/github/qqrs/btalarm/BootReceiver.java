package com.github.qqrs.btalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d("BootReceiver", "Boot received!");
		Intent startServiceIntent = new Intent(context, AlarmRegisterService.class);
        context.startService(startServiceIntent);
	}
}
