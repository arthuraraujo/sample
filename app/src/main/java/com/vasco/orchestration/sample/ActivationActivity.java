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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.authentication.UserAuthenticationCallback;
import com.vasco.orchestration.client.errors.OrchestrationErrorCodes;
import com.vasco.orchestration.client.flows.activation.online.OnlineActivationCallback;
import com.vasco.orchestration.client.flows.activation.online.OnlineActivationInputError;
import com.vasco.orchestration.client.flows.activation.online.OnlineActivationParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.CommandSender;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ActivationActivity extends AppCompatActivity implements OnlineActivationCallback {

    private static final String TAG = ActivationActivity.class.getName();

    private String userId;

    // UI components
    private EditText userIdView;
    private EditText activationPasswordView;
    private Dialog progressDialog;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        // Get handle on UI components
        userIdView = (EditText) findViewById(R.id.edit_user_id);
        activationPasswordView = (EditText) findViewById(R.id.edit_activation_password);
        activationPasswordView.setOnEditorActionListener(
                (v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        startRegistration();
                    }
                    return false;
                });

        // Register button
        Button registerBtn = (Button) findViewById(R.id.btn_register);
        registerBtn.setOnClickListener(
                view -> {

                    // Make sure to hide keyboard
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                    startRegistration();
                });

        // Get orchestrator
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(ActivationActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());
    }

    private void startRegistration() {

        // Get input strings
        userId = userIdView.getText().toString();
        String activationPassword = activationPasswordView.getText().toString();

        // Reset errors
        userIdView.setError(null);
        activationPasswordView.setError(null);

        // Create activation configuration
        OnlineActivationParams activationParams = new OnlineActivationParams();
        activationParams.setActivationCallback(this);
        activationParams.setOrchestrationUser(new OrchestrationUser(userId));
        activationParams.setActivationPassword(activationPassword);

        // Used for custom password instead of default one
        orchestrator.setUserAuthenticationCallback(
                orchestrationCallback,
                new UserAuthenticationCallback.UserAuthentication[] {
                    UserAuthenticationCallback.UserAuthentication.PASSWORD
                });

        // Show progress dialog & start activation
        progressDialog =
                UIUtils.displayProgress(this, getString(R.string.dialog_progress_activating));
        orchestrationCallback.setProgressDialog(progressDialog);
        orchestrator.startActivation(activationParams);
    }

    @Override
    public void onActivationStepComplete(String command) {

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
                                    ActivationActivity.this,
                                    getString(R.string.dialog_error_title),
                                    getString(R.string.dialog_error_content_sending));
                        } else orchestrator.execute(serverCommand);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
    }

    @Override
    public void onActivationSuccess() {

        // Hide progress
        UIUtils.hideProgress(progressDialog);

        // Store activated user identity in the local preferences
        SharedPreferencesStorage userStorage = new SharedPreferencesStorage(this);
        userStorage.setCurrentUser(userId);

        // Back to calling activity
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onActivationAborted() {

        // Hide progress
        UIUtils.hideProgress(progressDialog);

        // Display cancel message
        Log.d(TAG, "Activation cancelled by user");
        UIUtils.displayAlert(this, "Activation cancelled", "Activation cancelled by user");
    }

    @Override
    public void onActivationInputError(OnlineActivationInputError error) {

        // Hide progress
        UIUtils.hideProgress(progressDialog);

        // User got it wrong, display specific error
        Log.e(TAG, "Exception in onActivationError", error.getActivationInputException());
        switch (error.getErrorCode()) {
            case OrchestrationErrorCodes.USER_ID_NULL_OR_EMPTY:
                userIdView.setError("User ID is null or empty");
                break;
            case OrchestrationErrorCodes.USER_ID_WRONG_FORMAT:
                userIdView.setError("User ID contains invalid characters");
                break;
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_NULL_OR_EMPTY:
                activationPasswordView.setError("The activation password is null or empty");
                break;
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_WRONG_LENGTH:
                activationPasswordView.setError("The activation password has a wrong length");
                break;
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_WRONG_CHECKSUM:
                activationPasswordView.setError("The activation password is invalid");
                break;
            default:
                UIUtils.displayAlert(this, "Error", "Unknown error");
                break;
        }
    }
}
