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

import com.vasco.orchestration.client.cddc.CDDCDataFeeder;
import com.vasco.orchestration.client.cddc.CDDCParams;

import java.util.HashSet;
import java.util.Set;

public class CDDCUtils {
    private CDDCUtils() {}

    public static CDDCParams getCDDCParams() {
        // This data will be collected by the app and sent to the risk manager for decision making
        CDDCParams cddcParams = new CDDCParams();
        Set<CDDCParams.OptionalRetrievableFields> permissionFields = new HashSet<>();
        permissionFields.add(CDDCParams.OptionalRetrievableFields.WiFi);
        permissionFields.add(CDDCParams.OptionalRetrievableFields.Bluetooth);
        permissionFields.add(CDDCParams.OptionalRetrievableFields.Geolocation);
        cddcParams.setOptionalRetrievableFields(permissionFields);

        return cddcParams;
    }

    // To see full documentation of the setters, please refer to the integration guide or javadoc.
    public static void configure(CDDCDataFeeder cddcDataFeeder) {
        cddcDataFeeder.setApplicationReleaseDate(1501080258000L);
        cddcDataFeeder.setDeviceRooted(false);
        cddcDataFeeder.setRaspProtected(true);
    }
}
