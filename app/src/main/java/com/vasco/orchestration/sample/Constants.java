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

public class Constants {

    // The ACCOUNT_IDENTIFIER can be found on the Community Portal ( https://community.onespan.com )
    // by first hovering over your username and selecting 'Sandbox'. On this page should be a field
    // called 'Sandbox user'
    public static final String ACCOUNT_IDENTIFIER = "<account identifier>";

    public static final String CLOUD_SERVER_URL = ".sdb.tid.onespan.cloud";
    public static final String DOMAIN = ACCOUNT_IDENTIFIER.toLowerCase();
    // Adaptive Authentication web service URL
    // Add android:usesCleartextTraffic="true" to <application> tag in AndroidManifest
    // if you connecting to the server via http
    public static final String ENDPOINT_URL =
            "https://" + ACCOUNT_IDENTIFIER + CLOUD_SERVER_URL + "/v1/orchestration-commands";

    // Salts used to diversify the protection mechanisms for sensitive features. The values should
    // be random strings of 64 hexadecimal characters.
    public static final String SALT_STORAGE = "<salt storage>";
    public static final String SALT_DIGIPASS = "<salt digipass>";

    // JSON key for network requests
    public static final String SERVER_COMMAND_KEY = "command";
}
