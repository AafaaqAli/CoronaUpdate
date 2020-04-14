package com.gihub.coronavirusupdate;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import java.text.DecimalFormat;

class HelperClass {


    public boolean isNetworkAvailable(Context context) {


        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = false;
        for (Network network : connMgr.getAllNetworks()) {
            NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                isWifiConn |= networkInfo.isConnected();
            }
        }
        return isWifiConn;
    }

    public boolean isGPSActive(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        assert manager != null;
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isNetworkWorking(Context context) {
        if (isNetworkAvailable(context) && isGPSActive(context)) {
            try {
                String command = "ping -c 1 google.com";
                return (Runtime.getRuntime().exec(command).waitFor() == 0);
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public String convertToC(String kelvin) {
        if (kelvin != null) {

            double dblFahrenheit = Double.parseDouble(kelvin);
            double dblCelcius = dblFahrenheit - 273.15;
            // format
            DecimalFormat dfTenth = new DecimalFormat("#.##");

            return dfTenth.format(dblCelcius);
        } else {
            return "Error";
        }

    }
}
