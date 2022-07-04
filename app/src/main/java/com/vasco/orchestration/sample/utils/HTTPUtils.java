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

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLContext;

class HTTPUtils {

    private static final String TAG = HTTPUtils.class.getName();

    /**
     * Enable usage of TLSv1.2 regardless of the Android API Level See
     * https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
     */
    public static void enableTLSv1_2(Context applicationContext) {
        try {
            SSLContext.getInstance("TLSv1.2");
            ProviderInstaller.installIfNeeded(applicationContext);
        } catch (NoSuchAlgorithmException
                | GooglePlayServicesNotAvailableException
                | GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }
    }

    /** Performs a POST network request */
    private static String performRawPostNetworkRequestJson(
            String serverUrl, String content, boolean debug) throws IOException {
        return performRawPostNetworkRequestPrivate(serverUrl, content, debug);
    }

    /** Performs a POST network request */
    private static String performRawPostNetworkRequestPrivate(
            String serverUrl, String content, boolean debug) throws IOException {

        if (debug) {
            Log.d(TAG, "performRawPostNetworkRequest, request: " + serverUrl);
            Log.d(TAG, "performRawPostNetworkRequest, request params: " + content);
        }

        // Open connection
        URL url = new URL(serverUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setReadTimeout(30000);
        urlConnection.setConnectTimeout(15000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("charset", "utf-8");
        urlConnection.setRequestProperty(
                "Content-Length",
                Integer.toString(content.getBytes(StandardCharsets.UTF_8).length));

        DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
        wr.write(content.getBytes(StandardCharsets.UTF_8));
        wr.flush();
        wr.close();

        String response = getConnectionRawResponse(urlConnection);

        if (debug) Log.d(TAG, "performRawPostNetworkRequest, response: " + response);

        return response;
    }

    private static String getConnectionRawResponse(HttpURLConnection connection)
            throws IOException {
        StringBuilder total = new StringBuilder();
        try (BufferedReader r =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
        }

        return total.toString();
    }

    /** Performs a simple GET network request with json as input data */
    public static Map<String, String> performJSONRequest(
            String serverUrl, Map<String, String> content, boolean debug) throws Exception {
        // Perform network request with JSON key/values content
        JSONObject jsonKeyValue = new JSONObject(content);
        String respString =
                performRawPostNetworkRequestJson(serverUrl, jsonKeyValue.toString(), debug);

        // Parse the HTTP result
        return parseJsonKeyValueSerializedData(respString);
    }

    /**
     * Parses a serialized data (e.g. JSON key/values)
     *
     * @param data Data to parse
     * @return Map containing the parsed data
     */
    public static Map<String, String> parseJsonKeyValueSerializedData(String data)
            throws JSONException {
        Map<String, String> response = new HashMap<>();

        JSONObject json = new JSONObject(data.trim());
        Iterator<?> keys = json.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            String value = json.getString(key);
            response.put(key, value);
        }

        return response;
    }
}
