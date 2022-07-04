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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.vasco.orchestration.client.authentication.UserAuthenticationInputCallback;

import java.util.Objects;

@SuppressLint("ValidFragment")
public class UserAuthenticationDialogFragment extends DialogFragment {

    private final UserAuthenticationInputCallback inputCallback;
    private final Activity activity;
    private final boolean isEnrollment; // indicates if a new password must be set
    private int dialogMessage; // error message to display in case of password error
    private boolean confirmError; // indicates that new and confirm passwords don't match

    public UserAuthenticationDialogFragment(
            Activity activity,
            UserAuthenticationInputCallback inputCallback,
            boolean isEnrollment,
            int message) {
        this.activity = activity;
        this.inputCallback = inputCallback;
        this.isEnrollment = isEnrollment;
        this.dialogMessage = message;
        this.confirmError = false;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setPositiveButton(
                        "OK",
                        (dialog, id) -> {
                            EditText password = ((AlertDialog) dialog).findViewById(R.id.password);
                            EditText passwordConfirm =
                                    ((AlertDialog) dialog).findViewById(R.id.password_confirm);

                            if (isEnrollment
                                    && !Objects.requireNonNull(password)
                                            .getText()
                                            .toString()
                                            .equals(
                                                    Objects.requireNonNull(passwordConfirm)
                                                            .getText()
                                                            .toString())) {
                                dialogMessage = R.string.orch_pinpad_error_confirmation;
                                confirmError = true;
                                dismiss();
                                return;
                            }
                            inputCallback.onUserAuthenticationInputSuccess(
                                    Objects.requireNonNull(password).getText());
                            InputMethodManager imm =
                                    (InputMethodManager)
                                            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(
                                    Objects.requireNonNull(passwordConfirm).getWindowToken(), 0);

                            dismiss();
                        })
                .setNegativeButton(
                        "Cancel",
                        (dialog, id) -> {
                            inputCallback.onUserAuthenticationInputAborted();
                            dismiss();
                        })
                .setView(View.inflate(activity, R.layout.dialog_password, null))
                .setTitle(R.string.orch_pinpad_text_authentication);
        if (dialogMessage != 0) {
            builder.setMessage(dialogMessage);
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        if (!isEnrollment) {
            alertDialog.setOnShowListener(
                    dialogInterface -> {
                        View view =
                                ((AlertDialog) dialogInterface).findViewById(R.id.password_confirm);
                        Objects.requireNonNull(view).setVisibility(View.GONE);
                    });
        }

        return alertDialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        if (confirmError) {
            UserAuthenticationDialogFragment dialog =
                    new UserAuthenticationDialogFragment(
                            activity, inputCallback, isEnrollment, dialogMessage);
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.add(dialog, "dialog");
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        inputCallback.onUserAuthenticationInputAborted();
        dismiss();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.dialog_password, container, false);
    }
}
