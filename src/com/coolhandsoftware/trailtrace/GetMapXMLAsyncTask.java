package com.coolhandsoftware.trailtrace;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class GetMapXMLAsyncTask extends AsyncTask<MeasuredRoute, Void, ArrayList<GeoPoint>> {

	public interface ISnappedRouteReceiver {
		public abstract void storeSnappedRoute(ArrayList<GeoPoint> route);
	}
	
	ISnappedRouteReceiver mReceiver;
	Context mContext;
	
	public GetMapXMLAsyncTask(Context context, ISnappedRouteReceiver receiver) {
		mContext = context;
		mReceiver = receiver;
	}
	
	@Override
	protected void onPreExecute() {
		// display loading bar or something
	}
	
	@Override
	protected ArrayList<GeoPoint> doInBackground(MeasuredRoute... params) {
		// TODO
		return null;
	}
	
	@Override
	protected void onPostExecute(ArrayList<GeoPoint> results) {
		mReceiver.storeSnappedRoute(results);
	}
	
	/**
	 * Uses XMLPullParser to check XML from stream for OSM "ways", and "snap" the trace
	 * to a way or ways if close enough to the trace.
	 * 
	 * @param trace Original path from the map
	 * @param inStream Stream containing results from our XML map query
	 */
	private void parseXMLForWays(InputStream inStream) {
		
	}
	
	private String buildUrlForQuery(BoundingBoxE6 bbox) {
		
		// EXAMPLE FORMAT: "http://overpass-api.de/api/interpreter?data=(way(42.55,-71.49,42.59,-71.45)[highway];node(w));out skel;&"
		// bounding box syntax is (south, west, north, east)
		
		return null;
	}
	
	
	
	
	
	
	/**
	 * Opens an HttpUrlConnection to the given URL.
	 * 
	 * @param myUrl the complete URL to connect to
	 * @return the InputStream from the GET query, or null on failure
	 */
	private InputStream downloadURL(String myUrl) {
		
		InputStream inStream = null;
		
		NetworkChecker networkChecker = new NetworkChecker();
		if (networkChecker.hasNetworkConnectivity(mContext)) {
			
			try {
				
				
				
				
				
				
				
				
				
				
				URL url = new URL(myUrl);
				
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				
		        conn.setReadTimeout(10000 /* milliseconds */);
		        conn.setConnectTimeout(15000 /* milliseconds */);
		        conn.setRequestMethod("GET");
		        conn.setDoInput(true);
		        conn.connect();
		        int response = conn.getResponseCode();
		        Log.d("MapXMLDownload", "Downloading map XML, the response is: " + response);
		        inStream = conn.getInputStream();
			}
			catch (IOException e) {
				inStream = null;
				Log.d("MapXMLDownload", "Caught IOException while attempting to download map XML data");
			}
		}
		
		return inStream;
	} // end downloadUrl(str)
}
