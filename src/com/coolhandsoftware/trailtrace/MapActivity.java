package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView.OnFirstLayoutListener;
import org.osmdroid.views.Projection;

import com.coolhandsoftware.topogen.R;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

/**
 * This is the launcher activity for the application. It manages the map itself, as well as the route
 * drawing screen. Switching between the two is done with fragment transactions. It is declared to be
 * in launchMode="singleTop" in XML, so the user has only one map screen. 
 * 
 * The state variables for the map and the route drawing screen are stored in the singleton DrawnPathManager
 * object. This way, all the state is preserved in one place.Since there are lots of different sized pieces 
 * of data to preserve, pushing it all back to one place makes it easier to keep track of (read: debug).
 * 
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class MapActivity extends Activity implements IRouteDrawReceiver, OnFirstLayoutListener, INoNetworkDialogListener {

	/** convenience reference for class functions after onCreate **/
	public DrawnPathManager mDrawnPathManager;

	/** convenience reference for class functions after onCreate **/
	private MapFragment mMapFragment;
	
	/** convenience reference for class functions after onCreate **/
	private RouteDrawFragment mRouteDrawFragment;
	
	/** used to update icon after toggling route draw fragment **/
	private Menu mMenu;
	
	/** flag checked in OnFirstLayoutListener callback from MapView to determine whether to redraw route **/
	private boolean routeDrawRefreshScheduled = true;
	
	/** flag for callback to flip if no network connection found in onResume **/
	private boolean hasNetwork = false;
	
	/** tag used for no network dialog **/
	private final static String NO_NETWORK_DIALOG = "com.coolhandsoftware.NETWORK_DIALOG";
	
	/**
	 * Inflates, initializes convenience variables, registers to receive traces
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {        
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.map_activity);
        
        FragmentManager fm = getFragmentManager();
        mRouteDrawFragment = (RouteDrawFragment) fm.findFragmentById(R.id.route_draw_fragment);
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        mDrawnPathManager = DrawnPathManager.getInstance();
        
        mRouteDrawFragment.registerAsRouteDrawReceiver(this); 
    }
    
    /**
     * Shows a dialog informing the user that there's no network connection, and asking what to do.
     */
    private void launchNoNetworkDialog() {
        DialogFragment frag = new NoNetworkDialogFragment();
        frag.show(getFragmentManager(), NO_NETWORK_DIALOG);
    }
    
    /**
     * Tries again to find the network.
     * @see com.coolhandsoftware.trailtrace.INoNetworkDialogListener#onDialogPosClick(android.app.DialogFragment)
     */
	public void onDialogPosClick(DialogFragment dialog) {
		// do nothing - hasNetwork is set to false, still
		dialog.dismiss();
	}
	
	/**
	 * Gives up trying to find the network, and exits.
	 * @see com.coolhandsoftware.trailtrace.INoNetworkDialogListener#onDialogNegClick(android.app.DialogFragment)
	 */
	public void onDialogNegClick(DialogFragment dialog) {
		dialog.dismiss();
		finish();
	}
    
    /**
     * Begins the process of restoring state from mDrawnPathManager. Some stuff has to wait
     * for the osmdroid.MapView class to finish inflating (on a callback function). 
     * 
     * Also makes sure we have network connectivity.
     */
    @Override
    protected void onResume() {
    	super.onResume();    
    	
    	while (!hasNetwork) {
	    	// test for network connectivity
	        ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	        if (connectivity == null) {
	            launchNoNetworkDialog();
	        } 
	        else {
	            NetworkInfo[] info = connectivity.getAllNetworkInfo();
	            if (info != null) {
	                for (int i = 0; i < info.length; i++) {
	                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
	                    	hasNetwork = true;
	                        break;
	                    }
	                }
	                if (!hasNetwork) {
	                	launchNoNetworkDialog();
	                }
	            }
	            else {
	            	launchNoNetworkDialog();
	            }
	        }
    	}
    	
        // the draw screen is shown by default so we only need to hide it if its closed
        FragmentManager fm = getFragmentManager();
        if (!mDrawnPathManager.isRouteDrawOpen()) {
        	fm.beginTransaction().hide(mRouteDrawFragment).commit();
        }   
        
        // restore map to previous zoom level
        mMapFragment.setZoomLevel(mDrawnPathManager.getZoomLevel());
                
        // just refresh both of them, for now
        scheduleRouteDrawRefreshOnMapViewLaidOut();
        refreshTraceOnMap();
        
        // always do this to make sure the pencil icon is in sync, the search view is collapsed, etc
        invalidateOptionsMenu();
    }
    
    /**
     * Sets route draw reflesh flag to true.
     */
    private void scheduleRouteDrawRefreshOnMapViewLaidOut() {
    	routeDrawRefreshScheduled = true;
    }
    
	/**
	 * Store the map's current state and whether the route draw fragment is opened.
	 */
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (mRouteDrawFragment.isHidden()) {
    		mDrawnPathManager.setRouteDrawOpen(false);
    	}
    	else {
    		mDrawnPathManager.setRouteDrawOpen(true);
    	}
    	
    	mDrawnPathManager.storeCurrentMapCenter(mMapFragment.getCurrentMapCenter()); 
    	mDrawnPathManager.storeZoomLevel(mMapFragment.getZoomLevel());
    }
    
    /**
     * Called when the mapview gets its layout. Used to delay updating the route trace until the projection
     * will be valid. Also moves the map to the correct location as specified in the intent, if necessary.
     * @param v The mapview.
     * @param left Pixel value of left edge
     * @param top Pixel value of top edge
     * @param right Pixel value of right edge
     * @param bottom Pixel value of bottom edge
     */
    public void onFirstLayout(View v, int left, int top, int right, int bottom) {
    	if (routeDrawRefreshScheduled) {
    		refreshRouteDrawTrace();
    		routeDrawRefreshScheduled = false;
    	}
    	
    	checkIntentForCoordinatesOrFailQuery();
    }
    
    /**
     * Calls super, updates activity's intent, and calls helper function to check it for coordinates.
     * Android reliably calls onResume after this function.
     */
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	setIntent(intent);
    }
    
    /**
     * Checks current intent for coordinates or a failed query string from SearchActivity.
     */
	private void checkIntentForCoordinatesOrFailQuery() {
			
		Intent intent = getIntent();
		
	    double lati = intent.getDoubleExtra(SearchActivity.INTENDED_LATITUDE, 200); // there is no 200 lat/lon 
	    double longi = intent.getDoubleExtra(SearchActivity.INTENDED_LONGITUDE, 200); // useful to check if the extra exists
	    String failedquery = intent.getStringExtra(SearchActivity.NO_RESULTS_FOUND);
	    
	    if (lati != 200 && longi != 200) {
	    	// if we find an intended place to move to, move to it, and tell the mDrawnPathManager we did so
	    	mMapFragment.moveMapToLocation(lati, longi);
	    	mDrawnPathManager.storeCurrentMapCenter(new GeoPoint(lati, longi));
	    	intent.removeExtra(SearchActivity.INTENDED_LATITUDE);
	    	intent.removeExtra(SearchActivity.INTENDED_LONGITUDE);
	    }
	    else if (failedquery != null) {
	    	// if we tried to find a place to go but couldn't say so, and move to last known location
			Toast.makeText(this, "No results found for " + failedquery, Toast.LENGTH_LONG).show();
			IGeoPoint curSpot = mDrawnPathManager.getCurrentMapCenter(this);
			mMapFragment.moveMapToLocation(curSpot.getLatitude(), curSpot.getLongitude());
			intent.removeExtra(SearchActivity.NO_RESULTS_FOUND);
	    }
	    else {
	    	// just go to where we were before
			IGeoPoint curSpot = mDrawnPathManager.getCurrentMapCenter(this);
			mMapFragment.moveMapToLocation(curSpot.getLatitude(), curSpot.getLongitude());
	    }
	}
    
	/**
	 * Refreshes user-drawn trace from most recently stored set of raw user-inputted coordinates.
	 */
    private void refreshRouteDrawTrace() {
        if (mDrawnPathManager.hasStoredPoints()) {
        	// get them, and send them to the RouteDrawFragment for drawing
        	
        	Projection projection = mMapFragment.getCurrentProjection();
        	ArrayList<ArrayList<Pair<Integer, Integer>>> storedLineSegments = mDrawnPathManager.getStoredCoordPixels(projection);
        	mRouteDrawFragment.drawRouteFromPixels(storedLineSegments);
        }    	
    }
    
    /**
     * Refreshes the last processed, measured route rendered to the map from mDrawnPathManager.
     */
    private void refreshTraceOnMap() {
    	if (mDrawnPathManager.hasLastMeasuredRoute()) {
    		mMapFragment.drawRouteFromGeoPoints(mDrawnPathManager.getLastMeasuredRoute());
    	}
    }
    
    /**
     * Sets up options menu w/ search, makes sure route draw fragment icon is in sync with its fragment.
     * Also stores a reference to the menu in mMenu (so that toggleRouteDrawFragmentVisibility can update it later).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        
        // set up the search widget in the action bar
        MenuItem item = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName compName = new ComponentName(this, SearchActivity.class);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(compName));
        searchView.setIconifiedByDefault(true);
        
        if (!mDrawnPathManager.isRouteDrawOpen()) {
    		MenuItem drawMenuItem = menu.findItem(R.id.action_traceroute); 
    		drawMenuItem.setIcon(R.drawable.ic_menu_edit_disabled); // set icon to closed one
        }
        else {
    		MenuItem drawMenuItem = menu.findItem(R.id.action_traceroute);
    		drawMenuItem.setIcon(R.drawable.ic_menu_edit_enabled); // set icon to open one
        }
        
        mMenu = menu;
        return true;
    }
    
    /**
     * Note that search selections are handled by Android system - not here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId())
        {
        	case R.id.action_settings:
        		launchHelpActivity();
        		return true;
        	case R.id.action_traceroute:
        		toggleRouteDrawFragmentVisibility();
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * This shows/hides the route drawing fragment in or out of the activity
     * in order to enable or disable the route tracing feature. It also changes
     * the menu icon's appearance appropriately.
     */ 
    private void toggleRouteDrawFragmentVisibility() {
    	
    	FragmentManager fm = getFragmentManager();
    	if (mRouteDrawFragment.isHidden()) {
    		
    		fm.beginTransaction().show(mRouteDrawFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    		
    		MenuItem drawMenuItem = mMenu.findItem(R.id.action_traceroute);
    		drawMenuItem.setIcon(R.drawable.ic_menu_edit_enabled); // set icon to open one
    		
    		// forget about what was on the map before 
    		mDrawnPathManager.forgetLastMeasuredRoute();
    		mMapFragment.eraseTracedRoute();
    		
    		if (mDrawnPathManager.hasLastDrawSpot()) { // if it has stored points, move back to them
        		double lat = mDrawnPathManager.getLastDrawSpot().getLatitude();
        		double lon = mDrawnPathManager.getLastDrawSpot().getLongitude();
        		mMapFragment.moveMapToLocation(lat, lon);
        		refreshRouteDrawTrace();
    		}
    	}
    	else {
    		fm.beginTransaction().hide(mRouteDrawFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    		MenuItem drawMenuItem = mMenu.findItem(R.id.action_traceroute); 
    		drawMenuItem.setIcon(R.drawable.ic_menu_edit_disabled); // set icon to closed one
    	}
    }
    
    private void launchHelpActivity()
    {
    	Intent intent = new Intent(this, HelpActivity.class);
    	startActivity(intent);
    }
        
    /**
     * Pass coordinates to mDrawnPathManager.
     * @see com.coolhandsoftware.trailtrace.IRouteDrawReceiver#storeXyCoordinates(java.util.ArrayList)
     */
	public void storeXyCoordinates(ArrayList<Pair<Integer, Integer>> coordinates) {
		mDrawnPathManager.storeXyCoordinates(coordinates, mMapFragment.getCurrentProjection());
	}
	
	/**
	 * Tells mDrawnPathManager to forget the current trace.
	 * @see com.coolhandsoftware.trailtrace.IRouteDrawReceiver#eraseButtonPressed()
	 */
	public void eraseButtonPressed() {
		mDrawnPathManager.forgetStoredXyCoordinates();
		// note: the RouteDrawFragment calls the erase functions in the view
	}
	
	/**
	 * Tells mDrawnPathManager to process current coordinates, then passes them to the map for drawing,
	 * shifting user focus to the map and clearing the path manager's memory of what the raw data
	 * used to be (since now the trace has been converted into a measured path).
	 *  
	 * @see com.coolhandsoftware.trailtrace.IRouteDrawReceiver#measureButtonPressed()
	 */
	public void measureButtonPressed() {
		
		ArrayList<GeoPoint> geoPointsList = mDrawnPathManager.processStoredXyCoordinates();
	
		if (geoPointsList.size() > 0) {
			mMapFragment.drawRouteFromGeoPoints(geoPointsList);
			toggleRouteDrawFragmentVisibility();
			mDrawnPathManager.forgetStoredXyCoordinates();
		}
	}
}
