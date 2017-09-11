package app.voip;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.app.NotificationCompat;

import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;

public class RegistrationReceiver extends BroadcastReceiver{
    public static final String SIP_DISCONNECTED = "sip_disconnected";
    public static final String SIP_CONNECTED = "sip_connected";
    public static final String SIP_NO_INTERNET = "sip_no_internet";
    private VoIP mVoip;

    public RegistrationReceiver() {
        mVoip = VoIP.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)){
            NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);

            if(args == null){
                return;
            }

            switch(args.getEventType()){
                case REGISTRATION_NOK:
                    break;
                case UNREGISTRATION_OK:
                    createNotification(context, SIP_DISCONNECTED);
                    break;
                case REGISTRATION_OK:
                    createNotification(context, SIP_CONNECTED);
                    break;
                case REGISTRATION_INPROGRESS:
                    break;
                case UNREGISTRATION_INPROGRESS:
                    break;
                case UNREGISTRATION_NOK:
                    break;
                default:
            }
        } else {
            final ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifi.isAvailable()) {
                if(mVoip.isRegistered()) {
                    createNotification(context, SIP_CONNECTED);
                } else {
                    createNotification(context, SIP_DISCONNECTED);
                }
            } else {
                createNotification(context, SIP_NO_INTERNET);
            }
        }
    }

    public static void createNotification(Context context, String status) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context.getApplicationContext())
                        .setContentTitle(context.getResources().getString(R.string.sip_status))
                        .setOngoing(true);

        if(status.equals(SIP_CONNECTED)) {
            mBuilder.setSmallIcon(R.drawable.sip_connected);
            mBuilder.setContentText(context.getResources().getString(R.string.sip_connected));
        } else if(status.equals(SIP_DISCONNECTED)) {
            mBuilder.setSmallIcon(R.drawable.sip_disconnected);
            mBuilder.setContentText(context.getResources().getString(R.string.sip_disconnected));
        } else if(status.equals(SIP_NO_INTERNET)) {
            mBuilder.setSmallIcon(R.drawable.sip_disconnected);
            mBuilder.setContentText(context.getResources().getString(R.string.no_internet_access));
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(4856321, mBuilder.build());
    }
}

