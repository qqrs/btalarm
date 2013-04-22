package com.github.qqrs.btalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class AlarmReceiver extends BroadcastReceiver
{
	private static final String TAG = "AlarmReceiver";
	
    // Alarm events
	public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
	public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
	public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
	public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
	
	public static void register(Context context)
	{
		// This is a boot or the first time the user opens the app.
		// We need to start listening for the alarms
		IntentFilter filter = new IntentFilter(ALARM_ALERT_ACTION);
		filter.addAction(ALARM_DISMISS_ACTION);
		filter.addAction(ALARM_SNOOZE_ACTION);
		filter.addAction(ALARM_DONE_ACTION);

		BtAlarmApplication app = (BtAlarmApplication) context.getApplicationContext();

		context.registerReceiver(app.getAlarmReceiver(), filter);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Intent startServiceIntent = new Intent(context, AlarmRingerService.class);
        startServiceIntent.setAction(intent.getAction());
        context.startService(startServiceIntent);
	}
}
