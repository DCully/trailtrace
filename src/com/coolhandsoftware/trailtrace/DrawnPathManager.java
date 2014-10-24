package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.util.Pair;

/**
 * This class manages the traces drawn in the RouteDrawFragment and switching in between pixels and GeoPoints.
 * It also smoothes the line that the user draws when the measure button is pressed.
 * 
 * This object preserves the state of the points by extending the Fragment class and setting retain instance to true.
 * 
 * @author David Cully
 *
 */
public class DrawnPathManager {
	
	/** the most recent raw user inputted coordinates, stored as lat/lon pairs **/
	private ArrayList<ArrayList<Pair<Double, Double>>> mLatLonCoordsList = new ArrayList<ArrayList<Pair<Double, Double>>>();
	
	/** whether the route draw fragment is on top or not (default is not) **/
	private boolean routeDrawOpen = false;
	
	/** last spot where something was drawn on the map **/
	private double mLastLat = Double.NaN;
	
	/** last spot where something was drawn on the map **/
	private double mLastLon = Double.NaN;
	
	/** default zoom level for the map **/
	private int mZoomLevel = 11;
	
	/** last stored map center  **/
	private IGeoPoint mMapCenter = null;
	
	/** last list of points after processing from user input for use on the map **/
	private ArrayList<GeoPoint> mostRecentGeoPointsList = new ArrayList<GeoPoint>();
	
	/** sets smallest angle between points tolerated by segment merging algorithm **/
	final double MIN_ANGLE_DEGREES = 90.0; 
	
	/** for singleton pattern **/
	private static DrawnPathManager mSingleton = null;
    
	/** for singleton pattern **/
    public static DrawnPathManager getInstance() {
    	if (mSingleton == null) {
    		mSingleton = new DrawnPathManager();
    	}
    	return mSingleton;
    }
    
    /** private for singleton pattern **/
    private DrawnPathManager() {
    	
    }
    
    public void storeCurrentMapCenter(IGeoPoint center) {
    	mMapCenter = center;
    }
    
    /**
     * Returns last stored map center, or the user's current location if nothing was stored.
     * @param activity the Activity that wants the map center (typically the caller)
     * @return the last stored center of map
     */
    public IGeoPoint getCurrentMapCenter(Activity activity) {
    	if (mMapCenter == null) {
			LocationManager locMan = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			Location lastSpot = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			mMapCenter = new GeoPoint(lastSpot);
    	}
    	return mMapCenter;
    }
    
    public void storeZoomLevel(int zoom) {
    	mZoomLevel = zoom;
    }
    
    public int getZoomLevel() {
    	return mZoomLevel;
    }
    
    public void setRouteDrawOpen(boolean mapstate) {
    	routeDrawOpen = mapstate;
    }
    
    public boolean isRouteDrawOpen() {
    	return routeDrawOpen;
    }

    /**
     * This throws away any stored coordinates we may have received from the route draw fragment.
     * Called when the user presses the "erase" button.
     */
    public void forgetStoredXyCoordinates() {
    	mLatLonCoordsList.clear();
    	mLastLat = Double.NaN;
    	mLastLon = Double.NaN;
    }
    
    /**
     * check if the points are -200,-200, i.e., there is no last spot in storage
     * @return
     */
    public GeoPoint getLastDrawSpot() {
    	return new GeoPoint(mLastLat, mLastLon);
    }
    
    public boolean hasLastDrawSpot() {
    	if (Double.isNaN(mLastLat) || Double.isNaN(mLastLon)) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    /**
     * Throws away stored processed coordinates.
     */
    public void forgetLastMeasuredRoute() {
    	mostRecentGeoPointsList.clear();
    }
    
    /**
     * Test if there's a measured route stored from a previous processStoredXyCoordinates call.
     * @return true or false
     */
    public boolean hasLastMeasuredRoute() {
    	if (mostRecentGeoPointsList.size() > 0) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    /**
     * 
     * @return the list of geopoints
     */
    public ArrayList<GeoPoint> getLastMeasuredRoute() {
    	return mostRecentGeoPointsList;
    }
    
	/**
	 * Called when the user is inputting coordinates via touch. Stores coordinates in lat/lon format pre-emptively.
	 * @param coordinates The raw screen coordinates, in pixels (x,y).
	 * @param projection The projection used to convert the coordinates to (latitude, longitude).
	 */
    public void storeXyCoordinates(ArrayList<Pair<Integer, Integer>> pixelCoordinates, Projection projection) {
    	
    	Pair<Integer, Integer> curPair;
    	GeoPoint curPoint = new GeoPoint(0,0);
    	double latitude = Double.NaN;
    	double longitude = Double.NaN;
    	mLatLonCoordsList.add(new ArrayList<Pair<Double, Double>>());
    	
    	for (int index = 0; index < pixelCoordinates.size(); ++index) {
    		
    		curPair = pixelCoordinates.get(index);
    		
    		// convert the pixel coordinates into lat/lon by putting them into a (reused) geopoint
    		projection.fromPixels(curPair.first, curPair.second, curPoint); 
    		
    		// get them out of the geopoint
    		latitude = curPoint.getLatitude();
    		longitude = curPoint.getLongitude();
    		
    		// and store those lat/lon coordinates in the last segment in the list
    		mLatLonCoordsList.get(mLatLonCoordsList.size() - 1).add(new Pair<Double, Double>(latitude, longitude));
    	}
    	
    	// store last drawn point
    	mLastLat = latitude;
    	mLastLon = longitude;
    }
    
    /**
     * Called when the user presses the "measure" button. Smoothes out stored coordinates, builds
     * GeoPoint list, and returns them.
     * 
     * @return The processed list of GeoPoints.
     */
    public ArrayList<GeoPoint> processStoredXyCoordinates() {
    	
    	smoothStoredSegmentEndingsInPlace();
    	
    	Pair<Double, Double> curPair;
    	ArrayList<GeoPoint> processedGeoPoints = new ArrayList<GeoPoint>();

    	for (int x = 0; x < mLatLonCoordsList.size(); ++x) {
    		for (int y = 0; y < mLatLonCoordsList.get(x).size(); ++y) {
    			curPair = mLatLonCoordsList.get(x).get(y);
    			processedGeoPoints.add(new GeoPoint(curPair.first, curPair.second));
    		}
    	}
    	
    	mostRecentGeoPointsList = processedGeoPoints; // store the state of the last drawn line
    	return processedGeoPoints;
    }
    
    /**
     * Converts stored list of lat/lon coordinates that represent the raw route trace back into screen
     * coordinates and returns them.
     * @param projection
     * @return
     */
    public ArrayList<ArrayList<Pair<Integer, Integer>>> getStoredCoordPixels(Projection projection) {
    	
    	ArrayList<ArrayList<Pair<Integer, Integer>>> storedCoordsAsPixels = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
    	Pair<Double, Double> curPair;
    	Point curPoint = new Point(0,0);
    	GeoPoint curGeoPoint = new GeoPoint(0,0);
    	
    	//System.out.println("In getStoredCoordPixels");
    	
    	for (int x = 0; x < mLatLonCoordsList.size(); ++x) {
    		storedCoordsAsPixels.add(new ArrayList<Pair<Integer, Integer>>());
    		
    		for (int y = 0; y < mLatLonCoordsList.get(x).size(); ++y) {
    			curPair = mLatLonCoordsList.get(x).get(y);
    			curGeoPoint = new GeoPoint(curPair.first, curPair.second);
    			projection.toPixels(curGeoPoint, curPoint);
    			storedCoordsAsPixels.get(x).add(new Pair<Integer, Integer>(curPoint.x, curPoint.y));
    			//System.out.println("    converted " + curPair.first + ", " + curPair.second + " to " + curPoint.x + ", " + curPoint.y);
    		}
    	}
    	
    	return storedCoordsAsPixels;
    }
    
	public boolean hasStoredPoints() {
		return !mLatLonCoordsList.isEmpty();
	}
	
	/** HELPER FUNCTIONS **/	
	
	/**
	 * This function returns the angle formed by the three points passed as parameters.
	 * 
	 * @param first The first point (the end of one vector).
	 * @param second The second point (the vertex of both vectors).
	 * @param third The third point (the end of the other vector).
	 * @return An angle measurement between 0 and 180 degrees.
	 */
	private double angleFormedByPoints(Pair<Double, Double> first, Pair<Double, Double> second, Pair<Double, Double> third) {
		
		// This uses the law of cosines to find the angle (en.wikipedia.org/wiki/Law_of_cosines)
		// NOTE: I'm using all the same variable names as seen in the wikipedia article's first picture
		
		// these are the X and Y of the middle point (the one which forms the vertex of the angle)
		double Cx = second.first;
		double Cy = second.second;
		
		// these are the two endpoints of our two vectors A and B that come out of the middle point
		double Ax = first.first;
		double Ay = first.second;
		
		double Bx = third.first;
		double By = third.second;
		
		// first, we find the length of vectors A, B, and C using the distance formula
		double ALengthSquared = (Ax - Cx)*(Ax - Cx) + (Ay - Cy)*(Ay - Cy);
		double BLengthSquared = (Bx - Cx)*(Bx - Cx) + (By - Cy)*(By - Cy);
		double CLengthSquared = (Ax - Bx)*(Ax - Bx) + (Ay - By)*(Ay - By);
		
		// then we use the law of cosines to calculate the angle
		double cosineOfTheta = (CLengthSquared - ALengthSquared - BLengthSquared)/(-2*Math.sqrt(ALengthSquared)*Math.sqrt(BLengthSquared));
		double theta = Math.acos(cosineOfTheta);		
		theta = Math.toDegrees(theta);
		
		if (Double.isNaN(theta)) {
			theta = 180.0; // close enough, amirite
		}
				
		return theta;
	}

	
	private void smoothStoredSegmentEndingsInPlace() {
		for (int x = 0; x < mLatLonCoordsList.size() - 1; ++x) {
			ArrayList<Pair<Double, Double>> firstSegment = mLatLonCoordsList.get(x);
			ArrayList<Pair<Double, Double>> secondSegment = mLatLonCoordsList.get(x+1);
			
			while (firstSegment.size() >= 3 && secondSegment.size() >= 2 && 
				angleFormedByPoints(firstSegment.get(firstSegment.size()-1), secondSegment.get(0), secondSegment.get(1)) < 90 ) {
				
				// remove last two points from the end of the first segment
				firstSegment.remove(firstSegment.size()-1);
				firstSegment.remove(firstSegment.size()-1);
			}
		}
	}
}
