package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.coolhandsoftware.topogen.R;


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
public class MapActivity extends Activity implements RouteDrawView.IRouteDrawReceiver, 
														NoNetworkDialogFragment.INoNetworkDialogListener, 
														View.OnLayoutChangeListener, // used to delay drawing to map until it is laid out 
														SnappablePolyline.IPolylineDoubleTapReceiver 
														{

	/** convenience reference for class functions after onCreate **/
	private MapFragment mMapFragment;
	
	/** convenience reference for class functions after onCreate **/
	private RouteDrawFragment mRouteDrawFragment;
	
	/** used to update icon after toggling route draw fragment **/
	private Menu mMenu;
	
	/** flag checked in OnFirstLayoutListener callback from MapView to determine whether to redraw route **/
	private boolean routeDrawRefreshScheduled = true;
	
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
        
        mMapFragment.registerAsPolylineDtapListener(this);
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
     * If no network found, launch help activity intent with exit flag to go to home screen.
     * @see com.coolhandsoftware.trailtrace.INoNetworkDialogListener#onDialogPosClick(android.app.DialogFragment)
     */
	public void onDialogPosClick(DialogFragment dialog) {
		dialog.dismiss();
		Intent intent = new Intent(this, HelpActivity.class);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
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
    	
    	// test for network connectivity
    	NetworkChecker networkChecker = new NetworkChecker();
    	boolean hasNetwork = networkChecker.hasNetworkConnectivity(getApplicationContext());
    	if (!hasNetwork) {
    		launchNoNetworkDialog();
    	}
    	
        // the draw screen is shown by default so we only need to hide it if its closed
        FragmentManager fm = getFragmentManager();
        if (!MapTraceCoordinateManager.getInstance().isRouteDrawOpen()) {
        	fm.beginTransaction().hide(mRouteDrawFragment).commit();
        }   
        
        // restore map to previous zoom level
        mMapFragment.setZoomLevel(MapTraceCoordinateManager.getInstance().getZoomLevel());
                
        checkIntentForCoordinatesOrFailQuery();
        
        if (MapTraceCoordinateManager.getInstance().isRouteDrawOpen() == true) {
        	scheduleRouteDrawRefreshOnMapViewLaidOut();
        }
        else {
        	refreshTraceOnMap();
        }
        
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
    		MapTraceCoordinateManager.getInstance().setRouteDrawOpen(false);
    	}
    	else {
    		MapTraceCoordinateManager.getInstance().setRouteDrawOpen(true);
    	}
    	
    	MapTraceCoordinateManager.getInstance().storeCurrentMapCenter((GeoPoint) mMapFragment.getCurrentMapCenter()); 
    	MapTraceCoordinateManager.getInstance().storeZoomLevel(mMapFragment.getZoomLevel());
    }
    
    /**
     * Called whenever MapView layout changes - used to delay route draw refresh until the Projection will be valid.
     * @param v
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @param oldLeft
     * @param oldTop
     * @param oldRight
     * @param oldBottom
     */
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    	
    	if (routeDrawRefreshScheduled && v.isLaidOut()) {
    		refreshRouteDrawTrace();
    		routeDrawRefreshScheduled = false;
    	}
    }
    
    /**
     * Check to ensure a network connection before we leave - if we lost it, exit app (there's nothing we can do without the network).
     */
    @Override
    public void startActivity(Intent intent) {
    	// test for network connectivity again, in case we're launching searchActivity and have lost network since onResume
    	if (intent.getAction() == Intent.ACTION_SEARCH) {
        	NetworkChecker networkChecker = new NetworkChecker();
        	boolean hasNetwork = networkChecker.hasNetworkConnectivity(getApplicationContext());
        	if (!hasNetwork) {
        		launchNoNetworkDialog();
        	}	
    	}
    	super.startActivity(intent);
    }
    
	/**
	 * Refreshes user-drawn trace from most recently stored set of raw user-inputted coordinates.
	 */
    private void refreshRouteDrawTrace() {
        if (MapTraceCoordinateManager.getInstance().hasStoredTouchPoints()) {
        	// get them, and send them to the RouteDrawFragment for drawing
        	Projection projection = mMapFragment.getCurrentProjection();
          	ArrayList<ArrayList<Point>> storedLineSegments = MapTraceCoordinateManager.getInstance().getStoredTouchPoints(projection);
        	mRouteDrawFragment.drawRouteFromPixels(storedLineSegments);
        }    	
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
	    	MapTraceCoordinateManager.getInstance().storeCurrentMapCenter(new GeoPoint(lati, longi));
	    	intent.removeExtra(SearchActivity.INTENDED_LATITUDE);
	    	intent.removeExtra(SearchActivity.INTENDED_LONGITUDE);
	    }
	    else if (failedquery != null) {
	    	// if we tried to find a place to go but couldn't say so, and move to last known location
			Toast.makeText(this, "No results found for " + failedquery, Toast.LENGTH_LONG).show();
			IGeoPoint curSpot = MapTraceCoordinateManager.getInstance().getCurrentMapCenter(this);
			mMapFragment.moveMapToLocation(curSpot.getLatitude(), curSpot.getLongitude());
			intent.removeExtra(SearchActivity.NO_RESULTS_FOUND);
	    }
	    else {
	    	// just go to where we were before
			IGeoPoint curSpot = MapTraceCoordinateManager.getInstance().getCurrentMapCenter(this);
			mMapFragment.moveMapToLocation(curSpot.getLatitude(), curSpot.getLongitude());
	    }
	}
    
    /**
     * Refreshes the last processed, measured route rendered to the map from mDrawnPathManager.
     */
    public void refreshTraceOnMap() {
    	if (MapTraceCoordinateManager.getInstance().hasStoredTouchPoints()) {
    		mMapFragment.drawRoute(MapTraceCoordinateManager.getInstance().getMeasuredPoints(mMapFragment.getCurrentProjection()));
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
        
        if (!MapTraceCoordinateManager.getInstance().isRouteDrawOpen()) {
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
    		    		
    		// erase the blue line on the map, but don't forget the points - we need them to redraw the route trace
    		refreshRouteDrawTrace();
    		mMapFragment.eraseTracedRoute();
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
	public void storePixelPoint(Point coordinate, boolean isNewSegment) {
		MapTraceCoordinateManager.getInstance().storeTouchPoint(coordinate, isNewSegment, mMapFragment.getCurrentProjection());
	}
	
	/**
	 * Tells mDrawnPathManager to forget the current trace.
	 * @see com.coolhandsoftware.trailtrace.IRouteDrawReceiver#eraseButtonPressed()
	 */
	public void onEraseButtonPressed() {
		MapTraceCoordinateManager.getInstance().forgetPoints();
		// note: the RouteDrawFragment calls the erase functions in the view
	}
	
	/**
	 * Tells mDrawnPathManager to process current coordinates, then passes them to the map for drawing,
	 * shifting user focus to the map and clearing the path manager's memory of what the raw data
	 * used to be (since now the trace has been converted into a measured path).
	 *  
	 * @see com.coolhandsoftware.trailtrace.IRouteDrawReceiver#measureButtonPressed()
	 */
	public void onMeasureButtonPressed() {
		
		Projection projection = mMapFragment.getCurrentProjection();
		MeasuredRoute route = MapTraceCoordinateManager.getInstance().getMeasuredPoints(projection);
	
		if (route.mPoints.size() > 0 && route.mPoints.get(0).size() > 0) {
			mMapFragment.drawRoute(route);
			toggleRouteDrawFragmentVisibility();
		}
	}
	
	/**
	 * Called when the polyline displaying the measured trace on the map has been double tapped by the user.
	 * This is when we "snap" the line to nearby routes, so we get that process started now in the DrawnPathManager.
	 */
	public void onPolylineDoubletapped() {
		if (MapTraceCoordinateManager.getInstance().hasStoredTouchPoints()) {
			MapTraceCoordinateManager.getInstance().snapTraceToWays(mMapFragment.getCurrentProjection().getBoundingBox(), this, this);
		}
	}
}
