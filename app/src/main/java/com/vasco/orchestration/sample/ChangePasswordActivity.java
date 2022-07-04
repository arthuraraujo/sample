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
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.authentication.UserAuthenticationCallback;
import com.vasco.orchestration.client.flows.CryptoAppIndex;
import com.vasco.orchestration.client.flows.PasswordError;
import com.vasco.orchestration.client.flows.change_password.ChangePasswordCallback;
import com.vasco.orchestration.client.flows.change_password.ChangePasswordParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.CommandSender;
import com.vasco.orchestration.sample.utils.ErrorUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChangePasswordActivity extends AppCompatActivity implements ChangePasswordCallback {

    // UI components
    private Dialog progressDialog;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;
    private String userIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.btn_change_password).setOnClickListener(this::onStartChangePassword);

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(ChangePasswordActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());

        TextView userTextView = findViewById(R.id.text_user_identifier);
        SharedPreferencesStorage storage = new SharedPreferencesStorage(this);
        this.userIdentifier = storage.getCurrentUser();
        userTextView.setText(userIdentifier);
    }

    @Override
    public void onChangePasswordSuccess() {
        UIUtils.hideProgress(progressDialog);
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_change_password),
                getString(R.string.dialog_content_change_password_success));
    }

    @Override
    public void onChangePasswordAborted() {
        UIUtils.hideProgress(progressDialog);
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_change_password),
                getString(R.string.dialog_content_change_password_abortion));
    }

    @Override
    public void onChangePasswordInputError(PasswordError error) {
        UIUtils.hideProgress(progressDialog);
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(this, error.getErrorCode()));
    }

    @Override
    public void onChangePasswordStepComplete(String command) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future =
                executor.submit(new CommandSender(command, getApplicationContext()));
        executor.submit(
                () -> {
                    try {
                        String serverCommand = future.get();
                        if (serverCommand == null) {
                            UIUtils.hideProgress(progressDialog);
                            UIUtils.displayAlert(
                                    ChangePasswordActivity.this,
                                    getString(R.string.dialog_error_title),
                                    getString(R.string.dialog_error_content_sending));
                        } else orchestrator.execute(serverCommand);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
    }

    public void onStartChangePassword(View view) {
        // Display progress dialog and start password changing
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_change_pwd));
        orchestrationCallback.setProgressDialog(progressDialog);

        // Used for custom password instead of default one
        orchestrator.setUserAuthenticationCallback(
                orchestrationCallback,
                new UserAuthenticationCallback.UserAuthentication[] {
                    UserAuthenticationCallback.UserAuthentication.PASSWORD
                });

        // Initializing params
        ChangePasswordParams passwordParams = new ChangePasswordParams();
        passwordParams.setOrchestrationUser(new OrchestrationUser(userIdentifier));
        passwordParams.setChangePasswordCallback(ChangePasswordActivity.this);
        passwordParams.setCryptoAppIndex(CryptoAppIndex.SECOND);

        orchestrator.startChangePassword(passwordParams);
    }
}
