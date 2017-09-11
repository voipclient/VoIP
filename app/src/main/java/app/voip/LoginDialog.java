package app.voip;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import static android.content.Context.MODE_PRIVATE;

public class LoginDialog extends Dialog {

    private ScrollView mLoginView;
    private LinearLayout mAdvancedOptionsLayout;
    private ImageView mShowAdvancedOptionsView;

    private EditText mDomainEditText;
    private EditText mLoginEditText;
    private EditText mUserPasswordEditText;
    private EditText mServerAddressEditText;
    private EditText mServerPortEditText;

    private Button mRestoreDefaultAdvancedSettingsButton;
    private Button mSaveSettingsButton;

    public LoginDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.sip_settings_title);
        setContentView(R.layout.dialog_login);

        mLoginView = (ScrollView) findViewById(R.id.loginScrollView);

        mAdvancedOptionsLayout = (LinearLayout) findViewById(R.id.advanced_login_layout);

        mShowAdvancedOptionsView  = (ImageView) findViewById(R.id.login_advanced_ImageView);
        mShowAdvancedOptionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mAdvancedOptionsLayout.getVisibility()) {
                    case View.GONE:
                        mAdvancedOptionsLayout.setVisibility(View.VISIBLE);
                        mLoginView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mLoginView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        },100);
                        mShowAdvancedOptionsView.setImageResource(R.drawable.arrow_down);
                        break;
                    case View.VISIBLE:
                        mAdvancedOptionsLayout.setVisibility(View.GONE);
                        mShowAdvancedOptionsView.setImageResource(R.drawable.arrow_right);
                        break;
                    default:
                }
            }
        });

        mLoginEditText = (EditText) findViewById(R.id.loginEditText);
        mUserPasswordEditText = (EditText) findViewById(R.id.passwordEditText);
        mDomainEditText = (EditText) findViewById(R.id.domainEditText);
        mServerAddressEditText = (EditText) findViewById(R.id.serverAddressEditText);
        mServerPortEditText = (EditText) findViewById(R.id.servePortEditText);

        mRestoreDefaultAdvancedSettingsButton = (Button) findViewById(R.id.restore_default_settings);
        mRestoreDefaultAdvancedSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDomainEditText.setText(VoIP.SIP_DEFAULT_DOMAIN);
                mServerAddressEditText.setText(VoIP.SIP_DEFAULT_SERVER_HOST);
                mServerPortEditText.setText(VoIP.SIP_DEFAULT_SERVER_PORT+"");
            }
        });

        mSaveSettingsButton = (Button) findViewById(R.id.save_settings);
        mSaveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings(v.getContext());
                dismiss();
            }
        });
    }

    private void saveSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(VoIP.SIP_SETTINGS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String prefsDomain = prefs.getString(VoIP.SIP_DOMAIN_VAR, null);
        String prefsServerAddress = prefs.getString(VoIP.SIP_SERVER_HOST_VAR, null);
        int prefsServerPort = prefs.getInt(VoIP.SIP_SERVER_PORT_VAR, -1);

        if(mLoginEditText.getText().length() > 0) {
            editor.putString(VoIP.SIP_USERNAME_VAR, mLoginEditText.getText().toString());
        }

        if(mUserPasswordEditText.getText().length() > 0) {
            editor.putString(VoIP.SIP_PASSWORD_VAR, mUserPasswordEditText.getText().toString());
        }

        if(mDomainEditText.getText().length() > 0) {
            editor.putString(VoIP.SIP_DOMAIN_VAR, mDomainEditText.getText().toString());
        } else if(prefsDomain != null){
            editor.putString(VoIP.SIP_DOMAIN_VAR, prefsDomain);
        } else {
            editor.putString(VoIP.SIP_DOMAIN_VAR, VoIP.SIP_DEFAULT_DOMAIN);
        }

        if(mServerAddressEditText.getText().length() > 0) {
            editor.putString(VoIP.SIP_SERVER_HOST_VAR, mServerAddressEditText.getText().toString());
        } else if(prefsServerAddress != null){
            editor.putString(VoIP.SIP_SERVER_HOST_VAR, prefsServerAddress);
        } else {
            editor.putString(VoIP.SIP_SERVER_HOST_VAR, VoIP.SIP_DEFAULT_SERVER_HOST);
        }

        if(mServerPortEditText.getText().length() > 0) {
            editor.putInt(VoIP.SIP_SERVER_PORT_VAR, Integer.parseInt(mServerPortEditText.getText().toString()));
        } else if(prefsServerPort != -1){
            editor.putInt(VoIP.SIP_SERVER_PORT_VAR, prefsServerPort);
        } else {
            editor.putInt(VoIP.SIP_SERVER_PORT_VAR, VoIP.SIP_DEFAULT_SERVER_PORT);
        }

        editor.apply();
    }
}
