/**
 * 
 */
package com.github.qqrs.btalarm;

import android.content.Context;

/**
 *
 */
public class RN41Gpio {

    public static final int CMD_BEGIN = 1;
    public static final int CMD_END = 2;
    public static final int CMD_ON = 3;
    public static final int CMD_OFF = 4;
    public static final int CMD_STATUS = 5;

    private static final String MSG_BEGIN = "$$$";
    private static final String MSG_END = "---\n";
    private static final String MSG_ON = "S&,0808\n";
    private static final String MSG_OFF = "S&,0800\n";
    private static final String MSG_STATUS = "g&\n";

    public static void sendCmd(Context context, BluetoothChatService service, int cmd) {
        String msg = null;

        switch (cmd) {
        case CMD_BEGIN:
            // TODO: check before sending
            msg = MSG_BEGIN;
            break;
        case CMD_END:
            msg = MSG_END;
            break;
        case CMD_ON:
            // TODO: check before sending
            msg = MSG_ON;
            break;
        case CMD_OFF:
            msg = MSG_OFF;
            break;
        case CMD_STATUS:
            msg = MSG_STATUS;
            break;
        }

        if (msg.length() > 0) {
            byte[] send = msg.getBytes();
            service.write(send);
        }
    }

}
