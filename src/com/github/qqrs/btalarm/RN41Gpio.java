/**
 * 
 */
package com.github.qqrs.btalarm;

import android.content.Context;
import android.widget.Toast;
import com.github.qqrs.btalarm.R;

/**
 *
 */
public class RN41Gpio {

    public static final int CMD_BEGIN = 1;
    public static final int CMD_END = 2;
    public static final int CMD_ON = 3;
    public static final int CMD_OFF = 4;
    public static final int CMD_STATUS = 5;

    public static final int GPIO_ON = 1;
    public static final int GPIO_OFF = 0;

    private static final String MSG_BEGIN = "$$$";
    private static final String MSG_END = "---\n";
    private static final String MSG_ON = "S&,0808\n";
    private static final String MSG_OFF = "S&,0800\n";
    private static final String MSG_STATUS = "g&\n";

    private BluetoothChatService mService;
    private Context mContext;

    private boolean mCmdModeActive = false;
    private boolean mGPIOActive = false;

    public void sendCmd(int cmd) {
        String msg = null;

        if (mService.getState() != BluetoothChatService.STATE_CONNECTED) {
            // TODO: connect now? retry later?
            Toast.makeText(mContext, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (cmd) {
        case CMD_BEGIN:
            // TODO: check before sending
            mCmdModeActive = true;
            msg = MSG_BEGIN;
            break;
        case CMD_END:
            mCmdModeActive = false;
            msg = MSG_END;
            break;
        case CMD_ON:
            // TODO: check before sending
            mGPIOActive = true;
            msg = MSG_ON;
            break;
        case CMD_OFF:
            mGPIOActive = true;
            msg = MSG_OFF;
            break;
        case CMD_STATUS:
            msg = MSG_STATUS;
            break;
        }

        if (msg.length() > 0) {
            byte[] send = msg.getBytes();
            mService.write(send);
        }
    }

    public void setGpio(int state) {
        switch (state) {
        case GPIO_ON:
            sendCmd(CMD_ON);
            break;
        case GPIO_OFF:
            sendCmd(CMD_OFF);
            break;
        }
    }

	public RN41Gpio(BluetoothChatService service, Context context) {
        mService = service;
        mContext = context;
	}

    /*
    public void onStop() {
        if (mCmdModeActive) {
            if (mGPIOActive) {
                sendCmd(CMD_OFF);
            }
            sendCmd(CMD_END);
        }
    }
    */

}
