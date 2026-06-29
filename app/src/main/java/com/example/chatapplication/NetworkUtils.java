package com.example.chatapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtils {
    public static boolean isInternetConnected(Context context){
        ConnectivityManager cm  = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm==null){
            return false;
        }
        Network network = cm.getActiveNetwork();
        if(network==null){
            return false;
        }
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(network);
        if (networkCapabilities==null){
            return false;
        }
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
