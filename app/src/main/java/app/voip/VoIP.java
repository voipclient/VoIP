package app.voip;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnUriUtils;

import static android.content.Context.MODE_PRIVATE;

public class VoIP {
    private static VoIP sVoIPInstance = new VoIP();

    private NgnEngine mEngine;
    private INgnConfigurationService mConfigurationService;
    private INgnSipService mSipService;

    public static final String EXTRA_SIP_SESSION_ID = "SipSessionId";
    public static final String EXTRA_SIP_INIT_CALL_STATE = "SipSessionInitCallState";
    public static final String INCOMING_CALL = "IncomingCall";
    public static final String OUTGOING_CALL = "OutgoingCall";

    public final static String SIP_SETTINGS = "SipSettings";
    public final static String SIP_DOMAIN_VAR = "SIP_DOMAIN_VAR";
    public final static String SIP_USERNAME_VAR = "SIP_USERNAME_VAR";
    public final static String SIP_PASSWORD_VAR = "SIP_PASSWORD_VAR";
    public final static String SIP_SERVER_HOST_VAR = "SIP_SERVER_HOST_VAR";
    public final static String SIP_SERVER_PORT_VAR = "SIP_SERVER_PORT_VAR";
    public final static String SIP_DEFAULT_DOMAIN = "sip2sip.info";
    public final static String SIP_DEFAULT_SERVER_HOST = "proxy.sipthor.net";
    public final static int SIP_DEFAULT_SERVER_PORT = 5060;

    private String mSimDomain;

    private VoIP(){
    }

    public static VoIP getInstance(){
        return sVoIPInstance;
    }

    public void init(Context context) {
        if(!isOnline(context)) {
            Toast.makeText(context,R.string.no_internet_access,Toast.LENGTH_SHORT).show();
            return;
        }

        CallActivity.setDoesCallExist(false);
        mEngine = NgnEngine.getInstance();
        mConfigurationService = mEngine.getConfigurationService();
        mSipService = mEngine.getSipService();
    }

    public void configureLoginPassword(Context context, String login, String password) {
        SharedPreferences prefs = context.getSharedPreferences(VoIP.SIP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(VoIP.SIP_USERNAME_VAR, login);
        editor.putString(VoIP.SIP_PASSWORD_VAR, password);

        editor.commit();
    }

    public void configureVoIP(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(VoIP.SIP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String prefsDomain = prefs.getString(VoIP.SIP_DOMAIN_VAR, null);
        String prefsServerAddress = prefs.getString(VoIP.SIP_SERVER_HOST_VAR, null);
        int prefsServerPort = prefs.getInt(VoIP.SIP_SERVER_PORT_VAR, -1);

        if(prefsDomain != null){
            editor.putString(VoIP.SIP_DOMAIN_VAR, prefsDomain);
        } else {
            editor.putString(VoIP.SIP_DOMAIN_VAR, VoIP.SIP_DEFAULT_DOMAIN);
        }

        if(prefsServerAddress != null){
            editor.putString(VoIP.SIP_SERVER_HOST_VAR, prefsServerAddress);
        } else {
            editor.putString(VoIP.SIP_SERVER_HOST_VAR, VoIP.SIP_DEFAULT_SERVER_HOST);
        }

        if(prefsServerPort != -1){
            editor.putInt(VoIP.SIP_SERVER_PORT_VAR, prefsServerPort);
        } else {
            editor.putInt(VoIP.SIP_SERVER_PORT_VAR, VoIP.SIP_DEFAULT_SERVER_PORT);
        }

        editor.apply();
    }

    public boolean sipConfiguration(Context context) {
        if(!isOnline(context)) {
            Toast.makeText(context,R.string.no_internet_access,Toast.LENGTH_SHORT).show();
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(SIP_SETTINGS, MODE_PRIVATE);
        String userName = prefs.getString(SIP_USERNAME_VAR, null);
        String userPassword = prefs.getString(SIP_PASSWORD_VAR, null);
        String domainName = prefs.getString(SIP_DOMAIN_VAR, null);
        mSimDomain = domainName;
        String serverName = prefs.getString(SIP_SERVER_HOST_VAR, null);
        int serverPort = prefs.getInt(SIP_SERVER_PORT_VAR, -1);

        if(userName == null || userName.length() < 1 ||
                userPassword == null || userPassword.length() < 1 ||
                domainName == null || serverName == null || serverPort == -1) {
            return false;
        }

        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, userName);
        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, String.format("sip:%s@%s", userName, domainName));
        mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, userPassword);
        mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, serverName);
        mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, serverPort);
        mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, domainName);
        mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_REGISTRATION_TIMEOUT, 3600);

        mConfigurationService.commit();
        return true;
    }

    public void setReceiversEnabled(Context context, boolean enabled) {
        int newState;

        if(enabled) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }

        PackageManager pm = context.getPackageManager();

        ComponentName callReceiver =
                new ComponentName(context,
                        CallReceiver.class);
        pm.setComponentEnabledSetting(
                callReceiver,
                newState,
                PackageManager.DONT_KILL_APP);

        ComponentName registrationReceiver =
                new ComponentName(context,
                        RegistrationReceiver.class);
        pm.setComponentEnabledSetting(
                registrationReceiver,
                newState,
                PackageManager.DONT_KILL_APP);

        ComponentName notificationReceiver =
                new ComponentName(context,
                        NotificationReceiver.class);
        pm.setComponentEnabledSetting(
                notificationReceiver,
                newState,
                PackageManager.DONT_KILL_APP);
    }

    public void startSipService(Context context) {
        if(!isOnline(context)) {
            Toast.makeText(context,R.string.no_internet_access,Toast.LENGTH_SHORT).show();
            return;
        }

        if(!mEngine.isStarted()){
            if(mEngine.start()){
            } else {
                Toast.makeText(context, R.string.starting_sip_error, Toast.LENGTH_LONG).show();
                return;
            }
        }

        mSipService.register(context);
    }

    public boolean createVoipCall(Context context, String sipId) {
        if(!isOnline(context)) {
            Toast.makeText(context,R.string.no_internet_access,Toast.LENGTH_SHORT).show();
            return false;
        }

        if(!mSipService.isRegistered() || !mEngine.isStarted()) {
            Toast.makeText(context,R.string.not_logged_in,Toast.LENGTH_SHORT).show();
            return false;
        }

        if(sipId == null || sipId.length() < 1) {
            return false;
        }

        final String validSipId = NgnUriUtils.makeValidSipUri(String.format("sip:%s@%s", sipId, mSimDomain));
        if(validSipId == null){
            return false;
        }

        NgnAVSession avSession = NgnAVSession.createOutgoingSession(mSipService.getSipStack(), NgnMediaType.Audio);

        Intent i = new Intent();
        i.setClass(context, CallActivity.class);
        i.putExtra(EXTRA_SIP_SESSION_ID, avSession.getId());
        i.putExtra(EXTRA_SIP_INIT_CALL_STATE, OUTGOING_CALL);
        context.startActivity(i);

        return avSession.makeCall(validSipId);
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return (netInfo != null && netInfo.isConnected());
    }

    public void unregisterSip(Context context) {
        mSipService.unRegister();
        RegistrationReceiver.createNotification(context,RegistrationReceiver.SIP_DISCONNECTED);
    }

    public void clearSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SIP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public boolean isRegistered() {
        return mSipService.isRegistered();
    }
}

