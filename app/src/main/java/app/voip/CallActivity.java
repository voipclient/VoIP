package app.voip;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnInviteSession;

import java.util.HashMap;

public class CallActivity extends AppCompatActivity {
    private ViewFlipper mViewFlipper;

    //Incoming call UI Part (IC)
    private TextView mICSecondParticipantSipNameView;
    private ImageView mAcceptIncomingCallView;
    private ImageView mRejectIncomingCallView;

    //Outgoing call UI Part (OG)
    private TextView mOGSecondParticipantSipNameView;
    private Button mCancelCallButton;
    private MediaPlayer mPlayer;

    //InProgress call UI Part (IP)
    private TextView mIPSecondParticipantSipNameView;
    private TextView mCallTimeTextView;
    private long startTime;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Button mEndCallButton;

    //Other
    private NgnEngine mEngine;
    private BroadcastReceiver mSipBroadCastRecv;
    private NgnAVSession mSession;
    private Vibrator mVibrator;
    private String mInitialCallState;
    private boolean mDidCallConnected;
    public static final String SECOND_PARTICIPANT_SIP_NAME = "SecondParticipantSipName";
    private String mSecondParticipantSipName;
    private static boolean sDoesCallExist;

    private static HashMap<String, MissedCall> sMissedCallsHashMap = new HashMap<>();

    public CallActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sDoesCallExist = true;
        mEngine = NgnEngine.getInstance();

        mSipBroadCastRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleSipEvent(intent);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
        registerReceiver(mSipBroadCastRecv, intentFilter);

        setContentView(R.layout.activity_call);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mViewFlipper  = (ViewFlipper) findViewById(R.id.call_view_flipper);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            mSession = NgnAVSession.getSession(extras.getLong(VoIP.EXTRA_SIP_SESSION_ID));
            mInitialCallState = extras.getString(VoIP.EXTRA_SIP_INIT_CALL_STATE);
        }

        if(mSession == null || mInitialCallState == null){
            sDoesCallExist = false;
            finish();
            return;
        }

        mSession.setContext(this);
        mSecondParticipantSipName = mSession.getRemotePartyDisplayName();

        switch (mInitialCallState) {
            case VoIP.INCOMING_CALL:
                mEngine.getSoundService().startRingTone();
                mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.incoming_call_layout)));
                mVibrator = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
                long[] pattern = {0, 200, 200};
                mVibrator.vibrate(pattern, 0);
                break;
            case VoIP.OUTGOING_CALL:
                mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.outgoing_call_layout)));
                mPlayer = MediaPlayer.create(this, R.raw.dial_tone);
                mPlayer.setLooping(true);
                mPlayer.start();
                break;
            default:
        }

        //Incoming call UI Part
        mICSecondParticipantSipNameView = (TextView) findViewById(R.id.sipIdIncoming);
        mICSecondParticipantSipNameView.setText(mSecondParticipantSipName);

        mAcceptIncomingCallView  = (ImageView) findViewById(R.id.accept_incoming_call);
        mAcceptIncomingCallView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSession !=null) {
                    mSession.acceptCall();
                } else {
                    sDoesCallExist = false;
                    finish();
                }
            }
        });

        mRejectIncomingCallView = (ImageView) findViewById(R.id.reject_incoming_call);
        mRejectIncomingCallView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSession != null) {
                    mSession.hangUpCall();
                } else {
                    sDoesCallExist = false;
                    finish();
                }
            }
        });

        //Outgoing call UI Part
        mOGSecondParticipantSipNameView = (TextView) findViewById(R.id.sipIdOutgoing);
        mOGSecondParticipantSipNameView.setText(mSecondParticipantSipName);

        mCancelCallButton = (Button) findViewById(R.id.cancel_outgoing_call_button);
        mCancelCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSession !=null) {
                    mSession.hangUpCall();
                    if(mPlayer != null) {
                        mPlayer.stop();
                    }
                } else {
                    sDoesCallExist = false;
                    finish();
                }
            }
        });

        //InProgress call UI Part
        mIPSecondParticipantSipNameView = (TextView) findViewById(R.id.sipIdInProgress);
        mIPSecondParticipantSipNameView.setText(mSecondParticipantSipName);

        mCallTimeTextView = (TextView) findViewById(R.id.call_timer);

        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                mCallTimeTextView.setText(String.format("%d:%02d", minutes, seconds));

                timerHandler.postDelayed(this, 500);
            }
        };

        mEndCallButton = (Button) findViewById(R.id.end_in_progress_call_button);
        mEndCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSession !=null) {
                    mSession.hangUpCall();
                } else {
                    sDoesCallExist = false;
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!processKeyDown(keyCode)) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    public boolean processKeyDown(int keyCode) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            if(mSession.onVolumeChanged((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))){
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mSession != null){
            final NgnInviteSession.InviteState callState = mSession.getState();
            if(callState == NgnInviteSession.InviteState.TERMINATING || callState == NgnInviteSession.InviteState.TERMINATED){
                sDoesCallExist = false;
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        if(mSipBroadCastRecv != null){
            unregisterReceiver(mSipBroadCastRecv);
            mSipBroadCastRecv = null;
        }

        if(mSession != null){
            mSession.hangUpCall();
            mSession.setContext(null);
        }

        if(mEngine != null) {
            mEngine.getSoundService().stopRingTone();
            mEngine.getSoundService().stopRingBackTone();
        }

        if(mVibrator != null) {
            mVibrator.cancel();
        }

        if(mInitialCallState.equals(VoIP.OUTGOING_CALL) && mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }

        sDoesCallExist = false;

        super.onDestroy();
    }

    private void handleSipEvent(Intent intent){
        if(mSession == null){
            sDoesCallExist = false;
            finish();
            return;
        }
        final String action = intent.getAction();
        if(NgnInviteEventArgs.ACTION_INVITE_EVENT.equals(action)){
            NgnInviteEventArgs args = intent.getParcelableExtra(NgnInviteEventArgs.EXTRA_EMBEDDED);
            if(args == null){
                sDoesCallExist = false;
                finish();
                return;
            }
            if(args.getSessionId() != mSession.getId()){
                return;
            }

            final NgnInviteSession.InviteState callState = mSession.getState();
            switch(callState){
                case REMOTE_RINGING:
                    if(mEngine != null) {
                        mEngine.getSoundService().startRingBackTone();
                    }
                    break;
                case EARLY_MEDIA:
                    break;
                case INCALL:
                    mDidCallConnected = true;
                    if(mEngine != null) {
                        mEngine.getSoundService().stopRingTone();
                        mEngine.getSoundService().stopRingBackTone();
                    }
                    if(mInitialCallState.equals(VoIP.OUTGOING_CALL) && mPlayer != null && mPlayer.isPlaying()) {
                        mPlayer.stop();
                    }

                    if(mInitialCallState.equals(VoIP.INCOMING_CALL) && mVibrator != null){
                        mVibrator.cancel();
                    }
                    mSession.setSpeakerphoneOn(false);

                    if(mViewFlipper == null) {
                        mViewFlipper = (ViewFlipper) findViewById(R.id.call_view_flipper);
                    }
                    mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(findViewById(R.id.inprogress_call_layout)));

                    startTime = System.currentTimeMillis();
                    if(timerHandler!=null) {
                        timerHandler.postDelayed(timerRunnable, 0);
                    }
                    break;
                case TERMINATING:
                    break;
                case TERMINATED:
                    if(mEngine != null) {
                        mEngine.getSoundService().stopRingTone();
                        mEngine.getSoundService().stopRingBackTone();
                    }

                    if(mInitialCallState.equals(VoIP.INCOMING_CALL) && mVibrator != null){
                        mVibrator.cancel();
                    }

                    if(timerHandler != null) {
                        timerHandler.removeCallbacks(timerRunnable);
                    }

                    if(mInitialCallState.equals(VoIP.OUTGOING_CALL) && mPlayer != null && mPlayer.isPlaying()) {
                        mPlayer.stop();
                    }

                    if(mInitialCallState.equals(VoIP.INCOMING_CALL) && !mDidCallConnected) {
                        addMissedCallNotification(this, mSecondParticipantSipName);

                        if(mVibrator != null) {
                            mVibrator.cancel();
                        }
                    }

                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    public static void addMissedCallNotification(Context context, String callerName) {
        if(sMissedCallsHashMap.containsKey(callerName)){
            sMissedCallsHashMap.get(callerName).incrementCount();
        } else {
            sMissedCallsHashMap.put(callerName, new MissedCall());
        }

        int idNotification = sMissedCallsHashMap.get(callerName).getNotificationId();
        int missedCallsCount = sMissedCallsHashMap.get(callerName).getMissedCallsCount();
        String notificationText = callerName;

        if(missedCallsCount > 1) {
            notificationText += " (" + missedCallsCount + ")";
        }

        Intent intentNotification = new Intent(context, NotificationReceiver.class);
        intentNotification.putExtra(SECOND_PARTICIPANT_SIP_NAME,callerName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intentNotification, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context.getApplicationContext())
                        .setSmallIcon(android.R.drawable.sym_call_missed)
                        .setContentTitle(context.getResources().getString(R.string.missed_call))
                        .setContentText(notificationText)
                        .setDeleteIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(idNotification, mBuilder.build());
    }

    public static void removeMissedCall(String key) {
        sMissedCallsHashMap.remove(key);
    }

    public static boolean doesCallExist() {
        return sDoesCallExist;
    }

    public static void setDoesCallExist(boolean doesCallExist) {
        sDoesCallExist = doesCallExist;
    }
}

