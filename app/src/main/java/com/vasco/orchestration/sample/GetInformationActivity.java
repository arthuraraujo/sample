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

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.client.user.UserInformation;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class GetInformationActivity extends AppCompatActivity {

    // Shared preferences
    private SharedPreferencesStorage storage;

    // Utils
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_information);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(GetInformationActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Get a handle on shared preferences
        storage = new SharedPreferencesStorage(this);

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());

        // Get current user information
        getUserInformation(storage.getCurrentUser());
    }

    @Override
    protected void onDestroy() {
        storage = null;
        orchestrationCallback = null;
        super.onDestroy();
    }

    public void getUserInformation(String userIdentifier) {

        // Get user information
        UserInformation userInfo =
                orchestrator
                        .getUserManager()
                        .getUserInformation(new OrchestrationUser(userIdentifier));
        ((TextView) findViewById(R.id.text_get_info_userIdentifier))
                .setText(Objects.requireNonNull(userInfo).getUserIdentifier());
        ((TextView) findViewById(R.id.text_get_info_digipassSerialNumber))
                .setText(Objects.requireNonNull(userInfo).getDigipassSerialNumber());
        ((TextView) findViewById(R.id.text_get_info_digipassSequenceNumberUnprotected))
                .setText(
                        String.valueOf(
                                Objects.requireNonNull(userInfo)
                                        .getDigipassSequenceNumberUnprotected()));
        ((TextView) findViewById(R.id.text_get_info_digipassSequenceNumberProtected))
                .setText(
                        String.valueOf(
                                Objects.requireNonNull(userInfo)
                                        .getDigipassSequenceNumberProtected()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        ((TextView) findViewById(R.id.text_get_info_usedTimeSeconds))
                .setText(
                        sdf.format(
                                new Date(
                                        1000
                                                * Objects.requireNonNull(userInfo)
                                                        .getUsedTimeSeconds())));
        ((TextView) findViewById(R.id.text_get_info_clientServerTimeShiftSeconds))
                .setText(
                        String.valueOf(
                                Objects.requireNonNull(userInfo)
                                        .getClientServerTimeShiftSeconds()));
    }
}
