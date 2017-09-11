package app.voip;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    public static final String SIP_ACTION = "sip_action";
    public static final String SIP_ACTION_LOGIN = "sip_action_login";
    public static final String SIP_ACTION_LOGOUT = "sip_action_logout";
    public static final String SIP_ACTION_CALL = "sip_action_call";
    public static final String SIP_ADDRESS = "sip_address";
    public static final String SIP_LOGIN = "sip_login";
    public static final String SIP_PASSWORD = "sip_password";

    private VoIP voip;
    private EditText mSipAddress;
    private Button mCallButton;
    private Button mLoginButton;
    private Button mSettingsButton;
    private Button mLogoutButton;
    private TextView mIconsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        voip = VoIP.getInstance();
        voip.init(this);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            switch (extras.getString(SIP_ACTION)) {
                case SIP_ACTION_LOGIN:
                    voip.configureLoginPassword(this, extras.getString(SIP_LOGIN),extras.getString(SIP_PASSWORD));
                    voip.configureVoIP(this);
                    VoIPLogin();
                    break;
                case SIP_ACTION_CALL:
                    voip.createVoipCall(this, extras.getString(SIP_ADDRESS));
                    break;
                case SIP_ACTION_LOGOUT:
                    VoIPLogout();
                    break;
            }

            finish();
        }

        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        mSipAddress = (EditText) findViewById(R.id.idSipAddressToCall);

        mCallButton = (Button)findViewById(R.id.call_button);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                voip.createVoipCall(v.getContext(), mSipAddress.getText().toString());
            }
        });

        mLoginButton = (Button)findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                VoIPLogin();
            }
        });

        mLogoutButton = (Button)findViewById(R.id.logout_button);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                VoIPLogout();
            }
        });

        mSettingsButton = (Button)findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LoginDialog cdd = new LoginDialog(v.getContext());
                cdd.show();
            }
        });

        mIconsUrl = (TextView) findViewById(R.id.icons_url);
        Linkify.addLinks(mIconsUrl, Linkify.WEB_URLS);
    }

    private void VoIPLogin() {
        voip.setReceiversEnabled(getApplicationContext(),true);
        voip.sipConfiguration(this);
        voip.startSipService(this);
    }

    private void VoIPLogout() {
        voip.unregisterSip(this);
        voip.setReceiversEnabled(getApplicationContext(),false);
    }
}

