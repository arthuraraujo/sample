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
import com.vasco.orchestration.client.flows.local_transaction.LocalTransactionCallback;
import com.vasco.orchestration.client.flows.local_transaction.LocalTransactionParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.ErrorUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public class LocalTransactionActivity extends AppCompatActivity
        implements LocalTransactionCallback {

    private static final String TAG = LocalTransactionActivity.class.getName();

    // UI components
    private TextView userIdView;
    private EditText beneficiaryView, ibanView, amountView;
    private Dialog progressDialog;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;
    private ProtectionType selectedProtectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_transaction);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userIdView = findViewById(R.id.text_user_identifier);
        userIdView.setText(new SharedPreferencesStorage(this).getCurrentUser());
        beneficiaryView = findViewById(R.id.edit_beneficiary);
        ibanView = findViewById(R.id.edit_iban);
        amountView = findViewById(R.id.edit_amount);

        findViewById(R.id.radio_no_password).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.radio_password).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.radio_biomteric).setOnClickListener(this::onProtectionTypeSelected);
        findViewById(R.id.btn_generate_otp).setOnClickListener(this::onSignTransaction);

        // Set default protection type
        RadioGroup protectionTypeRadioGroup = findViewById(R.id.radio_group_protection_type);
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
                                () -> new WeakReference<>(LocalTransactionActivity.this))
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
    public void onLocalTransactionSuccess(String signature, String hostCode) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(this, "Local Transaction", "The generated signature is: " + signature);
    }

    @Override
    public void onLocalTransactionAborted() {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this, "Local Transaction", "The local transaction process has been aborted");
    }

    @Override
    public void onLocalTransactionPasswordError(PasswordError error) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(this, error.getErrorCode()));
        Log.e(TAG, "Exception in onLocalTransactionPasswordError", error.getPasswordException());
    }

    public void onSignTransaction(View view) {

        // Reset text field errors
        beneficiaryView.setError(null);
        ibanView.setError(null);
        amountView.setError(null);

        // Get user id
        String userId = userIdView.getText().toString();
        if (userId.isEmpty()) {
            userIdView.setError("User ID must not be empty");
            return;
        }

        // Get beneficiary
        String beneficiary = beneficiaryView.getText().toString();
        if (!beneficiary.matches("[0-9a-zA-Z]{1,16}")) {
            beneficiaryView.setError(
                    "Beneficiary must be an alphanumeric string of 16 characters max");
            return;
        }

        // Get IBAN
        String iban = ibanView.getText().toString();
        if (!iban.matches("[0-9a-zA-Z]{1,16}")) {
            ibanView.setError("IBAN must be an alphanumeric string of 16 characters max");
            return;
        }

        // Get amount
        String amount = amountView.getText().toString();
        if (!amount.matches("[0-9]{1,16}")) {
            amountView.setError("Amount must be a numeric string of 16 digits max");
            return;
        }

        // Configure params for local transaction
        LocalTransactionParams localTransactionParams = new LocalTransactionParams();
        localTransactionParams.setOrchestrationUser(new OrchestrationUser(userId));
        localTransactionParams.setDataFields(Arrays.asList(beneficiary, iban, amount));
        localTransactionParams.setCryptoAppIndex(
                CryptoAppIndex.FOURTH); // Fourth crypto app is used for signing transactions
        localTransactionParams.setProtectionType(selectedProtectionType);
        localTransactionParams.setLocalTransactionCallback(this);

        // Used for custom password instead of default one
        orchestrator.setUserAuthenticationCallback(
                orchestrationCallback,
                new UserAuthenticationCallback.UserAuthentication[] {
                    UserAuthenticationCallback.UserAuthentication.PASSWORD
                });

        // Start local transaction
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_local_tran));
        orchestrationCallback.setProgressDialog(progressDialog);
        orchestrator.startLocalTransaction(localTransactionParams);
    }
}
