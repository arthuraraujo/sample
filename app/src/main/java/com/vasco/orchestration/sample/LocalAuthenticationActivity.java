// Copyright ® 2022 OneSpan North America, Inc. All rights reserved. 

 
/////////////////////////////////////////////////////////////////////////////
//
//
// This file is example source code. It is provided for your information and
// assistance. See your licence agreement for details and the terms and
// conditions of the licence which governs the use of the source code. By using
// such source code you will be accepting these terms and conditions. If you do
// not wish to accept these terms and conditions, DO NOT OPEN THE FILE OR USE
// THE SOURCE CODE.
//
// Note that there is NO WARRANTY.
//
//////////////////////////////////////////////////////////////////////////////


package com.vasco.orchestration.sample;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.authentication.UserAuthenticationCallback;
import com.vasco.orchestration.client.flows.CryptoAppIndex;
import com.vasco.orchestration.client.flows.PasswordError;
import com.vasco.orchestration.client.flows.ProtectionType;
import com.vasco.orchestration.client.flows.local_authentication.LocalAuthenticationCallback;
import com.vasco.orchestration.client.flows.local_authentication.LocalAuthenticationParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.ErrorUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;

public class LocalAuthenticationActivity extends AppCompatActivity
        implements LocalAuthenticationCallback {

    private static final String TAG = LocalAuthenticationActivity.class.getName();

    // UI components
    private TextView userIdView;
    private EditText challengeView;
    private Dialog progressDialog;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;
    private ProtectionType selectedProtectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_authentication);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.radio_password).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.radio_no_password).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.radio_biomteric).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.btn_generate_otp).setOnClickListener(this::onGenerateOtp);

        userIdView = findViewById(R.id.text_user_identifier);
        userIdView.setText(new SharedPreferencesStorage(this).getCurrentUser());
        challengeView = (EditText) findViewById(R.id.edit_challenge);

        // Set default protection type
        RadioGroup protectionTypeRadioGroup =
                (RadioGroup) findViewById(R.id.radio_group_protection_type);
        protectionTypeRadioGroup.check(R.id.radio_no_password);
        selectedProtectionType = ProtectionType.NO_PASSWORD;

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(
                                () -> new WeakReference<>(LocalAuthenticationActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());

        getSupportFragmentManager().addOnBackStackChangedListener(this::openOrCloseProgressDialog);
    }

    private void openOrCloseProgressDialog() {
        if (progressDialog == null) return;

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        } else {
            progressDialog.show();
        }
    }

    public void onProtectionTypeSelected(View view) {

        int id = view.getId();
        if (id == R.id.radio_no_password) {
            selectedProtectionType = ProtectionType.NO_PASSWORD;
        } else if (id == R.id.radio_password) {
            selectedProtectionType = ProtectionType.PASSWORD;
        } else if (id == R.id.radio_biomteric) {
            selectedProtectionType = ProtectionType.BIOMETRIC;
        }
    }

    @Override
    public void onLocalAuthenticationSuccess(String otp, String hostCode) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(this, "Local Authentication", "The generated OTP is: " + otp);
    }

    @Override
    public void onLocalAuthenticationAborted() {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this, "Local Authentication", "The local authentication process has been aborted");
    }

    @Override
    public void onLocalAuthenticationPasswordError(PasswordError error) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(this, error.getErrorCode()));
        Log.e(TAG, "Exception in onLocalAuthenticationPasswordError", error.getPasswordException());
    }

    public void onGenerateOtp(View view) {

        // Get user id
        String userId = userIdView.getText().toString();
        if (userId.isEmpty()) {
            userIdView.setError("User ID must not be empty");
            return;
        }

        // Make sure challenge has a correct length
        String challenge = challengeView.getText().toString();
        if (!challenge.isEmpty() && !challenge.matches("[0-9a-fA-F]{6}")) {
            challengeView.setError("Challenge must be a 6 characters hexadecimal string");
            return;
        }

        // Configure params for local authentication
        LocalAuthenticationParams localAuthenticationParams = new LocalAuthenticationParams();
        localAuthenticationParams.setOrchestrationUser(new OrchestrationUser(userId));
        if (challenge.isEmpty()) {
            localAuthenticationParams.setCryptoAppIndex(
                    CryptoAppIndex.SECOND); // Second crypto app is used when no challenge is needed
        } else {
            localAuthenticationParams.setChallenge(challenge);
            localAuthenticationParams.setCryptoAppIndex(
                    CryptoAppIndex.THIRD); // Third crypto app is used when challenge is needed
        }
        localAuthenticationParams.setProtectionType(selectedProtectionType);
        localAuthenticationParams.setLocalAuthenticationCallback(this);

        // Used for custom password instead of default one
        orchestrator.setUserAuthenticationCallback(
                orchestrationCallback,
                new UserAuthenticationCallback.UserAuthentication[] {
                    UserAuthenticationCallback.UserAuthentication.PASSWORD
                });

        // Start local authentication
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_local_auth));
        orchestrationCallback.setProgressDialog(progressDialog);
        orchestrator.startLocalAuthentication(localAuthenticationParams);
    }
}
