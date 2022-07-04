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
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vasco.orchestration.client.Orchestrator;
import com.vasco.orchestration.client.cddc.CDDCMessage;
import com.vasco.orchestration.client.cddc.CDDCMessageParams;
import com.vasco.orchestration.client.user.OrchestrationUser;
import com.vasco.orchestration.sample.utils.CDDCUtils;
import com.vasco.orchestration.sample.utils.SampleOrchestrationCallback;
import com.vasco.orchestration.sample.utils.SharedPreferencesStorage;

import java.lang.ref.WeakReference;

public class CDDCMessageActivity extends AppCompatActivity {

    // Shared preferences
    private SharedPreferencesStorage storage;

    // Utils
    private SampleOrchestrationCallback orchestrationCallback;

    // Orchestration
    private Orchestrator orchestrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cddc_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.btn_generate_cddc).setOnClickListener(this::onGenerteMessage);

        // Get orchestrator instance
        orchestrationCallback = new SampleOrchestrationCallback(this);
        Orchestrator.Builder builder = new Orchestrator.Builder();
        orchestrator =
                builder.setDigipassSalt(Constants.SALT_DIGIPASS)
                        .setStorageSalt(Constants.SALT_STORAGE)
                        .setContext(getApplicationContext())
                        .setActivityProvider(() -> new WeakReference<>(CDDCMessageActivity.this))
                        .setDefaultDomain(Constants.DOMAIN)
                        .setCDDCParams(CDDCUtils.getCDDCParams())
                        .setErrorCallback(orchestrationCallback)
                        .setWarningCallback(orchestrationCallback)
                        .build();

        // Get a handle on shared preferences
        storage = new SharedPreferencesStorage(this);

        // Set values for the CDDC when available
        CDDCUtils.configure(orchestrator.getCDDCDataFeeder());
    }

    @Override
    protected void onDestroy() {
        orchestrationCallback = null;
        super.onDestroy();
    }

    public void onGenerteMessage(View view) {
        // Get Current Orchestration User
        String userIdentifier = storage.getCurrentUser();
        OrchestrationUser user = new OrchestrationUser(userIdentifier);

        // Retrive encrypt information
        boolean encrypt = ((CheckBox) findViewById(R.id.encrypt)).isChecked();

        // Event Name
        // This code value is predefined, and need to be indicated by server side
        // Avalaible codes are visible in the CDDC Technical Documentation
        String eventName = "eventnamecode";

        // Application Data
        // This is an extra information ask by server side, the final content need to be indicated
        // by them.
        // The expected formatting is a JSON in string format
        String applicationData = "{}";

        // Create message parameters
        CDDCMessageParams params = new CDDCMessageParams(user, eventName, applicationData, encrypt);

        // Generate the message
        CDDCMessage message = orchestrator.getCDDCMessage(params);
        TextView cddcMessage = ((TextView) findViewById(R.id.label_cddc_message));
        if (message.getReturnCode() == 0) {
            cddcMessage.setText(message.getCddcMessage());
        } else {
            cddcMessage.setText(String.format("Return Code: %s", message.getReturnCode()));
        }
    }
}
