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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vasco.orchestration.client.authentication.UserAuthenticationCallback;
import com.vasco.orchestration.client.authentication.UserAuthenticationInputCallback;
import com.vasco.orchestration.client.errors.InputError;
import com.vasco.orchestration.client.errors.OrchestrationError;
import com.vasco.orchestration.client.errors.OrchestrationErrorCallback;
import com.vasco.orchestration.client.errors.OrchestrationServerError;
import com.vasco.orchestration.client.errors.OrchestrationWarning;
import com.vasco.orchestration.client.errors.OrchestrationWarningCallback;
import com.vasco.orchestration.sample.R;
import com.vasco.orchestration.sample.UserAuthenticationDialogFragment;

public class SampleOrchestrationCallback
        implements OrchestrationErrorCallback,
                OrchestrationWarningCallback,
                UserAuthenticationCallback {

    private final Context context;
    private Dialog progressDialog;
    private UserAuthenticationInputCallback inputCallback;

    public SampleOrchestrationCallback(Context context) {
        this.context = context;
    }

    @Override
    public void onOrchestrationError(OrchestrationError error) {
        // Hide progress & display error
        UIUtils.hideProgress(progressDialog);
        UIUtils.displayAlert(
                context,
                context.getString(R.string.dialog_error_title),
                ErrorUtils.getErrorMessage(context, error.getErrorCode()));
        Log.e(
                context.getClass().getName(),
                "Exception in onOrchestrationError",
                error.getException());
    }

    @Override
    public void onOrchestrationServerError(OrchestrationServerError error) {
        // Hide progress & display readable message
        UIUtils.hideProgress(progressDialog);
        UIUtils.displayAlert(
                context,
                context.getString(R.string.dialog_error_title),
                error.getReadableMessage());
        Log.e(
                context.getClass().getName(),
                "Payload in onOrchestrationServerError: " + error.getCustomPayload());
    }

    @Override
    public void onOrchestrationWarning(OrchestrationWarning warning) {
        String warningMessage = ErrorUtils.getWarningMessage(context, warning.getWarningCode());
        Log.w(context.getClass().getName(), warningMessage);
    }

    public void setProgressDialog(Dialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    // UserAuthenticationCallback
    @Override
    public void onUserAuthenticationRequired(
            UserAuthentication type,
            final UserAuthenticationInputCallback inputCallback,
            final boolean isEnrollment) {
        this.inputCallback = inputCallback;
        UserAuthenticationDialogFragment dialog =
                new UserAuthenticationDialogFragment(
                        (Activity) context, inputCallback, isEnrollment, 0);
        FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(dialog, "dialog");
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onUserAuthenticationInputError(InputError error) {
        UserAuthenticationDialogFragment dialog =
                new UserAuthenticationDialogFragment(
                        (Activity) context, inputCallback, true, R.string.orch_pinpad_error_weak);
        FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
        dialog.show(fragmentManager, "dialog");
    }
}
