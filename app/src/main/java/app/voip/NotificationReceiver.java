package app.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class NotificationReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if(extras != null) {
            CallActivity.removeMissedCall(extras.getString(CallActivity.SECOND_PARTICIPANT_SIP_NAME));
        }
    }
}
