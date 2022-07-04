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

import com.vasco.orchestration.client.errors.OrchestrationErrorCodes;
import com.vasco.orchestration.client.errors.OrchestrationWarningCodes;
import com.vasco.orchestration.sample.R;

public class ErrorUtils {

    public static String getErrorMessage(Context context, int errorCode) {

        switch (errorCode) {
            case OrchestrationErrorCodes.COMMAND_PARSING_ERROR:
                return context.getString(R.string.error_command_parsing_failure);
            case OrchestrationErrorCodes.ACTIVATION_ERROR:
                return context.getString(R.string.error_activation);
            case OrchestrationErrorCodes.LOCAL_AUTHENTICATION_ERROR:
                return context.getString(R.string.error_local_authentication);
            case OrchestrationErrorCodes.LOCAL_TRANSACTION_ERROR:
                return context.getString(R.string.error_local_transaction);
            case OrchestrationErrorCodes.REMOTE_AUTHENTICATION_ERROR:
                return context.getString(R.string.error_remote_authentication);
            case OrchestrationErrorCodes.REMOTE_TRANSACTION_ERROR:
                return context.getString(R.string.error_remote_transaction);
            case OrchestrationErrorCodes.NOTIFICATION_REGISTRATION_ERROR:
                return context.getString(R.string.error_notification_registration);
            case OrchestrationErrorCodes.CHANGE_PASSWORD_ERROR:
                return context.getString(R.string.error_change_password);
            case OrchestrationErrorCodes.MISSING_PERMISSION_LOCATION:
                return context.getString(R.string.error_missing_location_permission);
            case OrchestrationErrorCodes.MISSING_PERMISSION_BLUETOOTH:
                return context.getString(R.string.error_missing_bluetooth_permission);
            case OrchestrationErrorCodes.MISSING_PERMISSION_WIFI:
                return context.getString(R.string.error_missing_wifi_permission);
            case OrchestrationErrorCodes.USER_ID_NULL_OR_EMPTY:
                return context.getString(R.string.error_user_id_null);
            case OrchestrationErrorCodes.USER_ID_WRONG_FORMAT:
                return context.getString(R.string.error_user_id_invalid);
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_NULL_OR_EMPTY:
                return context.getString(R.string.error_activation_password_null);
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_WRONG_LENGTH:
                return context.getString(R.string.error_activation_wrong_length);
            case OrchestrationErrorCodes.ACTIVATION_PASSWORD_WRONG_CHECKSUM:
                return context.getString(R.string.error_activation_wrong_checksum);
            case OrchestrationErrorCodes.PASSWORD_NULL:
                return context.getString(R.string.error_pwd_null);
            case OrchestrationErrorCodes.PASSWORD_TOO_SHORT:
                return context.getString(R.string.error_pwd_too_short);
            case OrchestrationErrorCodes.PASSWORD_TOO_LONG:
                return context.getString(R.string.error_pwd_too_long);
            case OrchestrationErrorCodes.PASSWORD_WEAK:
                return context.getString(R.string.error_pwd_weak);
            case OrchestrationErrorCodes.PASSWORD_WRONG:
                return context.getString(R.string.error_pwd_wrong);
            case OrchestrationErrorCodes.PASSWORD_LOCK:
                return context.getString(R.string.error_pwd_locked);
            default:
                return context.getString(R.string.error_unknown);
        }
    }

    public static String getWarningMessage(Context context, int warningCode) {
        switch (warningCode) {
            case OrchestrationWarningCodes.UNKNOWN_WARNING:
                return context.getString(R.string.warning_internal_error);
            case OrchestrationWarningCodes.MISSING_BLUETOOTH_PERMISSION:
                return context.getString(R.string.warning_no_bluetooth_permission);
            case OrchestrationWarningCodes.MISSING_LOCATION_PERMISSION:
                return context.getString(R.string.warning_no_location_permission);
            case OrchestrationWarningCodes.MISSING_READ_PHONE_STATE_PERMISSION:
                return context.getString(R.string.warning_no_read_phone_state_permission);
            case OrchestrationWarningCodes.LOCATION_UNAVAILABLE:
                return context.getString(R.string.warning_no_location);
            case OrchestrationWarningCodes.MISSING_WIFI_PERMISSION:
                return context.getString(R.string.warning_no_wifi_permission);
            default:
                return context.getString(R.string.warning_unknown_error);
        }
    }
}
