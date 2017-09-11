package app.voip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession;

public class CallReceiver extends BroadcastReceiver{
    private VoIP voip;
    private static NgnAVSession connectedSession;

    public CallReceiver() {
        voip = VoIP.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)){
            NgnInviteEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
            if(args == null){
                return;
            }

            NgnAVSession avSession = NgnAVSession.getSession(args.getSessionId());
            if (avSession == null) {
                return;
            }

            final NgnInviteSession.InviteState callState = avSession.getState();

            switch(callState){
                case INCOMING:
                    if(!CallActivity.doesCallExist() || connectedSession == null || !connectedSession.getState().equals(NgnInviteSession.InviteState.INCALL)) {
                        Intent i = new Intent();
                        i.setClass(context, CallActivity.class);
                        i.putExtra(VoIP.EXTRA_SIP_SESSION_ID, avSession.getId());
                        i.putExtra(VoIP.EXTRA_SIP_INIT_CALL_STATE, VoIP.INCOMING_CALL);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                    } else {
                        avSession.hangUpCall();
                        CallActivity.addMissedCallNotification(context ,avSession.getRemotePartyDisplayName());
                    }
                    break;
                case INCALL:
                    connectedSession = avSession;
                    break;
                case TERMINATED:
                    if(connectedSession != null && connectedSession.equals(avSession)) {
                        connectedSession = null;
                    }
                    break;
            }
        }
    }
}

