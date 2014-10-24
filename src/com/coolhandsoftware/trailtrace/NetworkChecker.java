package com.coolhandsoftware.trailtrace;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Helper class with member function to check to make sure we have a network connection.
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class NetworkChecker {

	public NetworkChecker() {

	}
	
	/**
	 * Check to see if we have a network connection.
	 * @param context Context from which we are looking for the network 
	 * @return true if we have a connection, false if we don't
	 */
	public boolean checkNetworkConnection(Context context) {
    	// test for network connectivity
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } 
        else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                    	return true;
                    }
                }
            }
            return false;
    	}
	}

}
