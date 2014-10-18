package com.coolhandsoftware.trailtrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.bonuspack.location.GeocoderNominatim;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This class handles searching for locations via Nominatim.
 * It displays the search results, and when the user picks one,
 * launches the MapActivity with a GeoPoint to go to.
 */
public class SearchActivity extends ListActivity {

	private static final String ADDRESS_LIST_TAG = "com.coolhandsoftware.ADDRESS_LIST_TAG";
	public static final String INTENDED_LATITUDE = "com.coolhandsoftware.INTENDED_LATITUDE";
	public static final String INTENDED_LONGITUDE = "com.coolhandsoftware.INTENDED_LONGITUDE";
	public static final String NO_RESULTS_FOUND = "com.coolhandsoftware.NO_RESULTS_FOUND";
	private ArrayList<Address> mAddressList;
	private String query;
	
	/**
	 * Checks bundle and intent to determine where it's at in the search process.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        
        if (savedInstanceState != null && savedInstanceState.containsKey(ADDRESS_LIST_TAG)) {
        	// this avoids redoing the search upon screen change
        	List<Address> addressList = savedInstanceState.getParcelableArrayList(ADDRESS_LIST_TAG);
        	populateList(addressList);
        }
        else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	// this is when the search has first been performed
        	String requestedLocationName = intent.getStringExtra(SearchManager.QUERY);
        	new GeocoderAsyncTask(this).execute(requestedLocationName);
        }
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
  	}
	
	/**
	 * Saves address list in bundle to avoid redoing a search on rotation, etc.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArrayList(ADDRESS_LIST_TAG, mAddressList);
	}
	
	/**
	 * Fill the ListView with results. 
	 * @param addressList the list of results.
	 */
	private void populateList(List<Address> addressList) {
		if (addressList == null) {
			// then return to the MapActivity
			Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra(NO_RESULTS_FOUND, query);
			startActivity(intent);
		}
		else if (addressList.size() > 0) {
			mAddressList = (ArrayList<Address>) addressList;
			ListAdapter addressListAdapter = new AddressListAdapter(this, mAddressList);
			setListAdapter(addressListAdapter);
		}
		else {
			Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra(NO_RESULTS_FOUND, query);
			startActivity(intent);
		}
	}
	
	/** 
	 * Takes the user back to the map activity when a result is selected.
	 */
    @Override
	protected void onListItemClick(ListView i, View v, int position, long id)
	{
		// build an intent, put in the lat/lon of the selected address, and launch the MapActivity
    	
    	Address selection = (Address) getListView().getItemAtPosition(position);
    	
    	double lat = selection.getLatitude();
    	double lon = selection.getLongitude();
    	
    	Intent intent = new Intent(this, MapActivity.class);
    	intent.putExtra(INTENDED_LATITUDE, lat);
    	intent.putExtra(INTENDED_LONGITUDE, lon);

    	startActivity(intent);
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
        	startActivity(new Intent(this, MapActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * This class takes a String and queries Nominatim to geocode it
     * asynchronously. It returns a List of Addresses to the UI thread.
     * It handles all searching operations and passes the results back
     * to the outer class for formatting/display/selection by user.
     * 
     * @author David Cully
     * david.a.cully@gmail.com
     *
     */
    private class GeocoderAsyncTask extends AsyncTask<String, Void, List<Address>> {

    	Context context;
    	ProgressDialog myProgressDialog;
    	
    	public GeocoderAsyncTask(Context context)
    	{
    		this.context=context;
    	}
    	
    	/**
    	 * Runs on UI thread.
    	 */
    	@Override
    	protected void onPreExecute()
    	{
    		super.onPreExecute();
    		
    		// show loading bar 
    		myProgressDialog = new ProgressDialog(context);
    		myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    		myProgressDialog.setCancelable(false);
    		myProgressDialog.show();
    	}
    	
    	/**
    	 * Runs on background thread.
    	 */
    	protected List<Address> doInBackground(String... locationName)
    	{
       		GeocoderNominatim geocoder = new GeocoderNominatim(context);
       		query = locationName[0];
    		// attempt to geocode via Nominatim
    		try 
    		{
        		return geocoder.getFromLocationName(locationName[0], 20); 
    		}
    		catch (IOException e)
    		{
    			// alas alack no network for us
    			Toast toast = Toast.makeText(context, "Couldn't connect to the network", Toast.LENGTH_LONG);
    			toast.show();
    		}
    		
    		return null;
    	}
    	
    	/**
    	 * Runs on UI thread.
    	 */
    	@Override
    	protected void onPostExecute(List<Address> results)
    	{
    		super.onPostExecute(results);
    		
    		// delete the loading bar
    		myProgressDialog.dismiss();
    		
    		// display the results in the UI, if we got any
    		if (results != null) 
    		{
    			// just hand off the search results to the SearchActivity 
    			// this object has no idea how to format the information, etc
        		populateList(results); 
    		}
    		else {
    			// if we don't get any results, cry havoc and let slip the toasts of disappointment
    			populateList(null);
    		}
    	}
    } // end inner class GeocoderAsyncTask    
} // end class SearchActivity
