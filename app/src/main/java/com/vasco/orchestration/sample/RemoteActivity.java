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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vasco.digipass.sdk.utils.notification.client.NotificationSDKClient;
import com.vasco.digipass.sdk.utils.notification.client.exceptions.NotificationSDKClientException;
import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.authentication.UserAuthenticationCallback;
import com.vasco.orchestration.client.flows.PasswordError;
import com.vasco.orchestration.client.flows.remote_authentication.RemoteAuthenticationCallback;
import com.vasco.orchestration.client.flows.remote_transaction.RemoteTransactionCallback;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.CommandSender;
import com.vasco.orchestration.sample.utils.ErrorUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RemoteActivity extends AppCompatActivity
        implements RemoteAuthenticationCallback, RemoteTransactionCallback {

    private static final String TAG = RemoteActivity.class.getName();

    // UI Components
    private TextView subtitleText;
    private TextView titleText;
    private View loginView;
    private View transactionView;
    private Dialog progressDialog;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;
    private RemoteAuthenticationCallback.DisplayDataCaller remoteAuthDisplayDataCaller;
    private RemoteTransactionCallback.DisplayDataCaller remoteTranDisplayDataCaller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        subtitleText = findViewById(R.id.txt_subtitle);
        titleText = findViewById(R.id.txt_title);
        loginView = findViewById(R.id.layout_login_buttons);
        transactionView = findViewById(R.id.layout_transaction_buttons);
        resetUIFields();

        findViewById(R.id.btn_no_login).setOnClickListener(this::onRejectLoginRequest);
        findViewById(R.id.btn_yes_login).setOnClickListener(this::onAcceptLoginRequest);
        findViewById(R.id.btn_no_transaction).setOnClickListener(this::onRejectTransactionRequest);
        findViewById(R.id.btn_yes_transaction).setOnClickListener(this::onAcceptTransactionRequest);

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(RemoteActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());

        // Set up callbacks for remote authentication
        orchestrator.setRemoteAuthCallback(this);

        // Set up callbacks for remote transaction
        orchestrator.setRemoteTranCallback(this);

        // Used for custom password instead of default one
        orchestrator.setUserAuthenticationCallback(
                orchestrationCallback,
                new UserAuthenticationCallback.UserAuthentication[] {
                    UserAuthenticationCallback.UserAuthentication.PASSWORD
                });

        // Handle possible incoming notification
        checkIntentForNotification(getIntent());

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

    private void resetUIFields() {
        titleText.setText(getString(R.string.hi));
        subtitleText.setText(getString(R.string.hi_subtitle));

        loginView.setVisibility(View.INVISIBLE);
        transactionView.setVisibility(View.INVISIBLE);
    }

    private void checkIntentForNotification(Intent intent) {

        try {
            setIntent(null);

            // Make sure the intent contains a notification
            if (NotificationSDKClient.isVASCONotification(intent)) {

                // Make sure the UI is in the proper state
                resetUIFields();

                // Get the command from the notification
                String command = NotificationSDKClient.parseVASCONotification(intent);

                // Display progress dialog and execute received command
                progressDialog =
                        UIUtils.displayProgress(
                                this, getString(R.string.dialog_progress_remote_incoming));
                orchestrationCallback.setProgressDialog(progressDialog);
                orchestrator.execute(command);
            }
        } catch (NotificationSDKClientException e) {

            openOrCloseProgressDialog();
            UIUtils.displayAlert(
                    this,
                    getString(R.string.dialog_error_title),
                    getString(R.string.dialog_error_content_notification));
            Log.e(TAG, "Exception in checkIntentForNotification", e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Handle possible incoming notification
        checkIntentForNotification(intent);
    }

    // region RemoteAuthenticationCallback
    @Override
    public void onRemoteAuthenticationDisplayData(
            String dataToDisplay,
            RemoteAuthenticationCallback.DisplayDataCaller displayDataCaller) {

        // Hide progress dialog
        openOrCloseProgressDialog();

        // Keep reference on caller for later
        this.remoteAuthDisplayDataCaller = displayDataCaller;

        if (dataToDisplay.contains("#")) {
            // Parse data and display the host on which the user wants to connect
            String host = dataToDisplay.split("#")[1];
            String user = dataToDisplay.split("#")[0];
            subtitleText.setText(getString(R.string.want_to_sign_in, host));
            titleText.setText(getString(R.string.hi_user, user));
        } else {
            titleText.setText(getString(R.string.hi));
            subtitleText.setText(getString(R.string.please_sign));
        }

        // Display accept/reject buttons
        loginView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRemoteAuthenticationStepComplete(String command) {
        sendCommandToServer(command);
    }

    @Override
    public void onRemoteAuthenticationSuccess(SuccessSessionState state) {
        openOrCloseProgressDialog();

        String messageContent = "";
        if (state == SuccessSessionState.Accepted)
            messageContent = getString(R.string.dialog_content_remote_auth_success_signed_in);
        else if (state == SuccessSessionState.Refused)
            messageContent = getString(R.string.dialog_content_remote_auth_success_rejected);

        UIUtils.displayAlert(this, getString(R.string.dialog_title_remote_auth), messageContent);
        // Hide accept/reject buttons
        titleText.setVisibility(View.INVISIBLE);
        subtitleText.setText(messageContent);
    }

    @Override
    public void onRemoteAuthenticationSessionOutdated(
            RemoteAuthenticationCallback.SessionOutdatedReason reason) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_remote_session_outdated),
                getSessionOutdatedReasonString(reason));
        // Hide accept/reject buttons
        titleText.setVisibility(View.INVISIBLE);
        subtitleText.setText(getString(R.string.dialog_content_remote_session_outdated));
    }

    @NonNull
    protected String getSessionOutdatedReasonString(
            RemoteAuthenticationCallback.SessionOutdatedReason reason) {
        switch (reason) {
            case Expired:
                return getString(R.string.dialog_content_remote_session_expired);
            case Accepted:
                return getString(R.string.dialog_content_remote_session_accepted);
            case Refused:
                return getString(R.string.dialog_content_remote_session_refused);
            case Unknown:
            default:
                return "";
        }
    }

    @Override
    public void onRemoteAuthenticationAborted() {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_remote_auth),
                getString(R.string.dialog_content_remote_auth_abortion));
    }

    @Override
    public void onRemoteAuthenticationPasswordError(PasswordError passwordError) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(this, passwordError.getErrorCode()));
    }
    // endregion

    // region RemoteTransactionCallback
    @Override
    public void onRemoteTransactionDisplayData(
            String dataToDisplay, RemoteTransactionCallback.DisplayDataCaller caller) {

        // Hide progress dialog
        openOrCloseProgressDialog();

        // Keep reference on caller for later
        this.remoteTranDisplayDataCaller = caller;

        String displayableDataToDisplay = parseRemoteTransactionDataToDisplay(dataToDisplay);
        subtitleText.setText(getString(R.string.want_to_sign_data, displayableDataToDisplay));
        titleText.setText(getString(R.string.hi));

        // Display accept/reject buttons
        transactionView.setVisibility(View.VISIBLE);
    }

    private String parseRemoteTransactionDataToDisplay(String dataToDisplay) {
        String[] split = dataToDisplay.split("#");
        StringBuilder retStr = new StringBuilder();
        for (String s : split) retStr.append(s).append("\n");

        return retStr.toString();
    }

    @Override
    public void onRemoteTransactionStepComplete(String command) {
        sendCommandToServer(command);
    }

    @Override
    public void onRemoteTransactionSuccess(SuccessTransactionState state) {
        openOrCloseProgressDialog();
        String messageContent;
        if (state == SuccessTransactionState.Accepted)
            messageContent = getString(R.string.dialog_content_remote_tran_success_validated);
        else messageContent = getString(R.string.dialog_content_remote_tran_success_rejected);

        UIUtils.displayAlert(this, getString(R.string.dialog_title_remote_tran), messageContent);
        // Hide accept/reject buttons
        titleText.setVisibility(View.INVISIBLE);
        subtitleText.setText(messageContent);
    }

    @Override
    public void onRemoteTransactionSessionOutdated(
            RemoteTransactionCallback.SessionOutdatedReason reason) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_remote_session_outdated),
                getSessionOutdatedReasonString(reason));
        // Hide accept/reject buttons
        titleText.setVisibility(View.INVISIBLE);
        subtitleText.setText(getString(R.string.dialog_content_remote_session_outdated));
    }

    @NonNull
    protected String getSessionOutdatedReasonString(
            RemoteTransactionCallback.SessionOutdatedReason reason) {
        switch (reason) {
            case Expired:
                return getString(R.string.dialog_content_remote_session_expired);
            case Accepted:
                return getString(R.string.dialog_content_remote_session_accepted);
            case Refused:
                return getString(R.string.dialog_content_remote_session_refused);
            case Unknown:
            default:
                return "";
        }
    }

    @Override
    public void onRemoteTransactionAborted() {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_title_remote_tran),
                getString(R.string.dialog_content_remote_tran_abortion));
    }

    @Override
    public void onRemoteTransactionPasswordError(PasswordError passwordError) {
        openOrCloseProgressDialog();
        UIUtils.displayAlert(
                this,
                getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(this, passwordError.getErrorCode()));
    }
    // endregion

    private void sendCommandToServer(String command) {

        // Initialize custom async task for sending an orchestration command to the server
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
                                    RemoteActivity.this,
                                    getString(R.string.dialog_error_title),
                                    getString(R.string.dialog_error_content_sending));
                        } else {
                            orchestrator.execute(serverCommand);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
    }

    public void onRejectLoginRequest(View view) {

        // Hide accept/reject buttons
        subtitleText.setText(getString(R.string.hi_subtitle));
        loginView.setVisibility(View.INVISIBLE);

        // Display progress dialog and accept the login request
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_remote_auth));
        remoteAuthDisplayDataCaller.onDataRejected();
    }

    public void onAcceptLoginRequest(View view) {

        // Hide accept/reject buttons
        subtitleText.setText(getString(R.string.hi_subtitle));
        loginView.setVisibility(View.INVISIBLE);

        // Display progress dialog and reject the login request
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_remote_auth));
        remoteAuthDisplayDataCaller.onDataApproved();
    }

    public void onRejectTransactionRequest(View view) {

        // Hide accept/reject buttons
        subtitleText.setText(getString(R.string.hi_subtitle));
        transactionView.setVisibility(View.INVISIBLE);

        // Display progress dialog and accept the login request
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_remote_trans));
        remoteTranDisplayDataCaller.onDataRejected();
    }

    public void onAcceptTransactionRequest(View view) {

        // Hide accept/reject buttons
        subtitleText.setText(getString(R.string.hi_subtitle));
        transactionView.setVisibility(View.INVISIBLE);

        // Display progress dialog and reject the login request
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_remote_trans));
        remoteTranDisplayDataCaller.onDataApproved();
    }
}
