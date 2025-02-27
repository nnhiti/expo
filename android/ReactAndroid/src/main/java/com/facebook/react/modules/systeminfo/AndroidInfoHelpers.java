// Copyright (c) Facebook, Inc. and its affiliates.
package com.facebook.react.modules.systeminfo;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import com.facebook.common.logging.FLog;
import com.facebook.react.R;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Locale;

public class AndroidInfoHelpers {

    public static String EMULATOR_LOCALHOST = "10.0.2.2";

    public static String GENYMOTION_LOCALHOST = "10.0.3.2";

    public static String DEVICE_LOCALHOST = "localhost";

    public static String METRO_HOST_PROP_NAME = "metro.host";

    public static Integer sDevServerPortOverride = null;
    public static Integer sInspectorProxyPortOverride = null;

    public static String TAG = AndroidInfoHelpers.class.getSimpleName();

    private static boolean isRunningOnGenymotion() {
        return Build.FINGERPRINT.contains("vbox");
    }

    private static boolean isRunningOnStockEmulator() {
        return Build.FINGERPRINT.contains("generic");
    }

    public static String getServerHost(Integer port) {
        return getServerIpAddress(port);
    }

    public static String getServerHost(Context context) {
        return getServerIpAddress(getDevServerPort(context));
    }

    public static String getAdbReverseTcpCommand(Integer port) {
        return "adb reverse tcp:" + port + " tcp:" + port;
    }

    public static String getAdbReverseTcpCommand(Context context) {
        return getAdbReverseTcpCommand(getDevServerPort(context));
    }

    public static String getInspectorProxyHost(Context context) {
        return getServerIpAddress(getInspectorProxyPort(context));
    }

    // WARNING(festevezga): This RN helper method has been copied to another FB-only target. Any
    // changes should be applied to both.
    public static String getFriendlyDeviceName() {
        if (isRunningOnGenymotion()) {
            // Genymotion already has a friendly name by default
            return Build.MODEL;
        } else {
            return Build.MODEL + " - " + Build.VERSION.RELEASE + " - API " + Build.VERSION.SDK_INT;
        }
    }

    private static Integer getDevServerPort(Context context) {
        if (sDevServerPortOverride != null) {
          return sDevServerPortOverride;
        }
        Resources resources = context.getResources();
        return resources.getInteger(R.integer.react_native_dev_server_port);
    }

    private static Integer getInspectorProxyPort(Context context) {
        if (sInspectorProxyPortOverride != null) {
          return sInspectorProxyPortOverride;
        }
        Resources resources = context.getResources();
        return resources.getInteger(R.integer.react_native_dev_server_port);
    }

    public static void setDevServerPort(Integer port) {
      sDevServerPortOverride = port;
    }

    public static void setInspectorProxyPort(Integer port) {
      sInspectorProxyPortOverride = port;
    }

    private static String getServerIpAddress(int port) {
        // Since genymotion runs in vbox it use different hostname to refer to adb host.
        // We detect whether app runs on genymotion and replace js bundle server hostname accordingly
        String ipAddress;
        String metroHostProp = getMetroHostPropValue();
        if (!metroHostProp.equals("")) {
            ipAddress = metroHostProp;
        } else if (isRunningOnGenymotion()) {
            ipAddress = GENYMOTION_LOCALHOST;
        } else if (isRunningOnStockEmulator()) {
            ipAddress = EMULATOR_LOCALHOST;
        } else {
            ipAddress = DEVICE_LOCALHOST;
        }
        return String.format(Locale.US, "%s:%d", ipAddress, port);
    }

    public static String metroHostPropValue = null;

    private static synchronized String getMetroHostPropValue() {
        if (metroHostPropValue != null) {
            return metroHostPropValue;
        }
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/bin/getprop", METRO_HOST_PROP_NAME });
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));
            String lastLine = "";
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            metroHostPropValue = lastLine;
        } catch (Exception e) {
            FLog.w(TAG, "Failed to query for metro.host prop:", e);
            metroHostPropValue = "";
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception exc) {
            }
            if (process != null) {
                process.destroy();
            }
        }
        return metroHostPropValue;
    }
}
