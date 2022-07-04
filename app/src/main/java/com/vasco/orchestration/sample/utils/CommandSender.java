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


package com.vasco.orchestration.sample.utils;

import android.content.Context;
import android.util.Log;

import com.vasco.orchestration.sample.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class CommandSender implements Callable<String> {

    private static final String TAG = CommandSender.class.getName();
    private final String command;

    public CommandSender(String cmd, Context context) {
        this.command = cmd;
        HTTPUtils.enableTLSv1_2(context);
    }

    @Override
    public String call() {
        try {
            // Make HTTP call
            Map<String, String> request = new HashMap<>();
            request.put(Constants.SERVER_COMMAND_KEY, command);
            Map<String, String> serverResponse =
                    HTTPUtils.performJSONRequest(Constants.ENDPOINT_URL, request, true);
            if (!serverResponse.containsKey(Constants.SERVER_COMMAND_KEY)) {
                return null;
            }

            // Return received command
            return serverResponse.get(Constants.SERVER_COMMAND_KEY);
        } catch (Exception e) {
            Log.e(TAG, "Exception in SendCommandTask", e);
            return null;
        }
    }
}
