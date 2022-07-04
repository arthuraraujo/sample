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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.vasco.digipass.sdk.utils.notification.client.NotificationSDKClient;
import com.vasco.digipass.sdk.utils.notification.client.exceptions.NotificationSDKClientException;
import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.flows.notification.NotificationRegistrationCallback;
import com.vasco.orchestration.client.flows.notification.NotificationRegistrationParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.CommandSender;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;
import com.vasco.orchestration.sample.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements NotificationRegistrationCallback {

    private static final String TAG = MainActivity.class.getName();

    // Shared preferences
    private SharedPreferencesStorage storage;

    // UI components
    private View layoutActivated, layoutNotActivated;
    private Dialog progressDialog;
    private Spinner userSpinner;
    private ArrayAdapter<CharSequence> userAdapter;
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // We have 2 layouts, one when no user is activated, the other when a user is activated
        layoutActivated = findViewById(R.id.layout_activated);
        layoutNotActivated = findViewById(R.id.layout_not_activated);
        layoutActivated.setVisibility(View.INVISIBLE);
        layoutNotActivated.setVisibility(View.INVISIBLE);

        findViewById(R.id.btn_start_register).setOnClickListener(this::onStartActivationClicked);
        findViewById(R.id.btn_add).setOnClickListener(this::onAddUser);
        findViewById(R.id.btn_delete).setOnClickListener(this::onDeleteUser);
        findViewById(R.id.btn_la).setOnClickListener(this::onTryLocalAuthentication);
        findViewById(R.id.btn_lt).setOnClickListener(this::onTryLocalTransaction);
        findViewById(R.id.btn_cp).setOnClickListener(this::onDoChangePassword);
        findViewById(R.id.btn_gi).setOnClickListener(this::onTryGetInformation);
        findViewById(R.id.btn_cddc).setOnClickListener(this::onTryCDDCMessage);

        // Get a handle on shared preferences
        storage = new SharedPreferencesStorage(this);

        // Initialize spinner for selecting users
        userSpinner = findViewById(R.id.spinner_select_user);
        userSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int position, long id) {
                        String selectedUSer = adapterView.getItemAtPosition(position).toString();
                        storage.setCurrentUser(selectedUSer);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // Do nothing
                    }
                });

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(MainActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Handle possible incoming notification
        checkIntentForNotification(getIntent());
    }

    @Override
    protected void onDestroy() {
        orchestrationCallback = null;
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Handle possible incoming notification
        checkIntentForNotification(intent);
    }

    private void checkIntentForNotification(Intent intent) {
        try {

            // Make sure the intent contains a notification
            if (NotificationSDKClient.isVASCONotification(intent)) {

                // Get the command from the notification
                String command = NotificationSDKClient.parseVASCONotification(intent);

                // Display progress dialog and execute received command
                progressDialog =
                        UIUtils.displayProgress(
                                this, getString(R.string.dialog_progress_remote_auth));
                orchestrator.execute(command);
            }
        } catch (NotificationSDKClientException e) {

            UIUtils.hideProgress(progressDialog);
            UIUtils.displayAlert(
                    this,
                    getString(R.string.dialog_error_title),
                    getString(R.string.dialog_error_content_notification));
            Log.e(TAG, "Exception in checkIntentForNotification", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        // No activated user, show corresponding text & button
        String activatedUser = storage.getCurrentUser();
        if (activatedUser == null) {
            layoutActivated.setVisibility(View.GONE);
            layoutNotActivated.setVisibility(View.VISIBLE);
            return;
        }

        // Update user selection spinner
        List<CharSequence> userIdentifiers = new ArrayList<>();
        for (OrchestrationUser user : orchestrator.getUserManager().getUsers())
            userIdentifiers.add(user.getUserIdentifier());
        userAdapter = new ArrayAdapter<>(this, R.layout.spinner_layout, userIdentifiers);
        userSpinner.setAdapter(userAdapter);
        userSpinner.setSelection(userAdapter.getPosition(activatedUser));

        // We have an activated user, show proper layout
        layoutActivated.setVisibility(View.VISIBLE);
        layoutNotActivated.setVisibility(View.GONE);

        // Start registering for notifications
        registerForNotifications();
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission) {
        ActivityCompat.requestPermissions(this, new String[] {permission}, 0);
    }

    public void onStartActivationClicked(View view) {
        startActivity(new Intent(this, ActivationActivity.class));
    }

    public void onAddUser(View view) {
        startActivity(new Intent(this, ActivationActivity.class));
    }

    public void onDeleteUser(View view) {

        UIUtils.displayAlertWithAction(
                MainActivity.this,
                getString(R.string.confirm_delete_user_title),
                getString(R.string.confirm_delete_user_message)
                        + " "
                        + storage.getCurrentUser()
                        + "?",
                getString(R.string.confirm_delete_user_yes),
                getString(R.string.confirm_delete_user_no),
                (dialog, id) -> onDeleteUserInternal(),
                null);
    }

    private void onDeleteUserInternal() {
        // Delete current user
        OrchestrationUser orchestrationUser = new OrchestrationUser(storage.getCurrentUser());
        orchestrator.getUserManager().deleteUser(orchestrationUser);
        storage.removeNotificationIdForUser(storage.getCurrentUser());

        // Back on activation page if there are no more users
        List<OrchestrationUser> users = orchestrator.getUserManager().getUsers();
        if (users.size() == 0) {
            layoutActivated.setVisibility(View.GONE);
            layoutNotActivated.setVisibility(View.VISIBLE);
            storage.setCurrentUser(null);
            return;
        }
        // Update user selection spinner
        List<CharSequence> userIdentifiers = new ArrayList<>();
        for (OrchestrationUser user : users) userIdentifiers.add(user.getUserIdentifier());
        userAdapter = new ArrayAdapter<>(this, R.layout.spinner_layout, userIdentifiers);
        userSpinner.setAdapter(userAdapter);
        userAdapter.notifyDataSetChanged();
        storage.setCurrentUser(userSpinner.getSelectedItem().toString());
    }

    public void onTryLocalAuthentication(View view) {
        startActivity(new Intent(this, LocalAuthenticationActivity.class));
    }

    public void onTryLocalTransaction(View view) {
        startActivity(new Intent(this, LocalTransactionActivity.class));
    }

    public void onDoChangePassword(View view) {
        startActivity(new Intent(this, ChangePasswordActivity.class));
    }

    public void onTryGetInformation(View view) {
        startActivity(new Intent(this, GetInformationActivity.class));
    }

    public void onTryCDDCMessage(View view) {
        startActivity(new Intent(this, CDDCMessageActivity.class));
    }

    @SuppressWarnings("WrongConstant")
    private void registerForNotifications() {
        NotificationSDKClient.registerNotificationService(
                this,
                new NotificationSDKClient.NotificationSDKClientListener() {
                    @Override
                    public void onRegistrationSuccess(String notificationId) {
                        // Start registration for push notifications
                        List<OrchestrationUser> orchestrationUsers =
                                orchestrator.getUserManager().getUsers();
                        for (final OrchestrationUser orchestrationUser : orchestrationUsers) {
                            String storedNotificationId =
                                    storage.getStoredNotificationIdForUser(
                                            orchestrationUser.getUserIdentifier());
                            if (storedNotificationId == null
                                    || !storedNotificationId.equals(notificationId)) {
                                NotificationRegistrationParams notificationRegistrationParams =
                                        new NotificationRegistrationParams();
                                notificationRegistrationParams.setOrchestrationUser(
                                        orchestrationUser);
                                notificationRegistrationParams.setNotificationIdentifier(
                                        notificationId);
                                notificationRegistrationParams.setNotificationRegistrationCallback(
                                        MainActivity.this);
                                orchestrator.startNotificationRegistration(
                                        notificationRegistrationParams);
                                runOnUiThread(
                                        () ->
                                                Snackbar.make(
                                                                findViewById(android.R.id.content),
                                                                "Push notification registration"
                                                                        + " for "
                                                                        + orchestrationUser
                                                                                .getUserIdentifier(),
                                                                Snackbar.LENGTH_SHORT)
                                                        .show());
                            }
                        }
                    }

                    @Override
                    public void onException(NotificationSDKClientException e) {
                        Log.e(
                                TAG,
                                "Exception when retrieving notification Id: errorCode "
                                        + e.getErrorCode(),
                                e);
                    }
                });
    }

    @Override
    public void onNotificationRegistrationStepComplete(String command) {
        sendCommandToServer(command);
    }

    @Override
    @SuppressWarnings("WrongConstant")
    public void onNotificationRegistrationSuccess(
            OrchestrationUser orchestrationUser, String notificationIdentifier) {

        // Store notification ID in shared preferences & notify user
        storage.storeNotificationIdForUser(
                orchestrationUser.getUserIdentifier(), notificationIdentifier);
        Snackbar.make(
                        findViewById(android.R.id.content),
                        "Push notification registration success for "
                                + orchestrationUser.getUserIdentifier(),
                        Snackbar.LENGTH_SHORT)
                .show();
    }

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
                                    MainActivity.this,
                                    getString(R.string.dialog_error_title),
                                    getString(R.string.dialog_error_content_sending));
                        } else orchestrator.execute(serverCommand);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
    }
}
