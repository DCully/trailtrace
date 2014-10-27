package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.util.Pair;

/**
 * This is the model object for the program - it contains all persistent coordinate data, for simplicity.
 * Other objects get access to the data by adding themselves as TouchPointListeners or MeasuredRouteListeners. 
 * They get called with data whenever the state of the data changes. 
 * 
 * This class is a singleton, so that its instance survives the activity lifecycle. To get the instance, you have
 * to call the static member function MapTraceCoordinateManager.getInstance(), similar to elsewhere in Android.
 * 
 * @author David Cully (david.a.cully@gmail.com)
 *
 */
public class MapTraceCoordinateManager implements GetMapXMLAsyncTask.ISnappedRouteReceiver {

	/** whether the route draw fragment is on top or not (default is not) **/
	private boolean routeDrawOpen = false;
	
	/** default zoom level for the map **/
	private int mZoomLevel = 11;
	
	/** last stored map center  **/
	private GeoPoint mMapCenter = null;
	
	/** user-generated pixel coordinate pairs, stored as GeoPoints **/
	private ArrayList<ArrayList<GeoPoint>> mTouchPoints = new ArrayList<ArrayList<GeoPoint>>();
	
	/** for singleton pattern **/
	private static MapTraceCoordinateManager mSingleton = null;
	    
	/** for singleton pattern **/
    public static MapTraceCoordinateManager getInstance() {
    	if (mSingleton == null) {
    		mSingleton = new MapTraceCoordinateManager();
    	}
    	return mSingleton;
    }
    
    /** private for singleton pattern **/
    private MapTraceCoordinateManager() {
    	
    }
	
	/**
	 * Stores this point as the next one in a continuous user-generated trace.
	 * If end of segment, pass isEndOfSegment as true.
	 * @param point point to be stored
	 * @param isStartOfSegment flag to either store the point or delimit a segment
	 * @param projection current map projection
	 */
	public void storeTouchPoint(Point point, boolean isStartOfSegment, Projection projection) {
		
		GeoPoint gPoint = (GeoPoint) projection.fromPixels(point.x, point.y);
		if (isStartOfSegment) {
			mTouchPoints.add(new ArrayList<GeoPoint>());
			mTouchPoints.get(mTouchPoints.size()-1).add(gPoint);
		}
		else {
			mTouchPoints.get(mTouchPoints.size() - 1).add(gPoint);
		}
	}
	
	/**
	 * Converts stored GeoPoints back into Points.
	 * @return
	 */
	public ArrayList<ArrayList<Point>> getStoredTouchPoints(Projection projection) {
		
		ArrayList<ArrayList<Point>> result = new ArrayList<ArrayList<Point>>();
		
		for (int x = 0; x < mTouchPoints.size(); ++x) {
			result.add(new ArrayList<Point>());
			for (int y = 0; y < mTouchPoints.get(x).size(); ++y) {
				Point point = projection.toPixels(mTouchPoints.get(x).get(y), null);
				result.get(x).add(point);
			}
		}
		
		return result;
	}

	/**
	 * Erases all stored points.
	 */
	public void forgetPoints() {
		mTouchPoints.clear();
	}
	
	/**
	 * 
	 * @return true if there are points, false if there aren't
	 */
	public boolean hasStoredTouchPoints() {
		return !mTouchPoints.isEmpty();
	}
	
	/**
	 * Converts stored Points to GeoPoints, measures the route's length, and returns a MeasuredRoute.
	 * @param projection Used to convert Points to GeoPoints. 
	 * @return MeasuredRoute, contains route length and GeoPoints
	 */
	public MeasuredRoute getMeasuredPoints(Projection projection) {

		MeasuredRoute result = new MeasuredRoute(mTouchPoints, measureRoute(mTouchPoints));
		return result;
	}
	

	/**
	 * Measures the length of a route, in miles, by directly linking the segments together.
	 * @param route GeoPoints that make up the route.
	 * @return mileage
	 */
	private float measureRoute(ArrayList<ArrayList<GeoPoint>> route) {
		float result = 0;
		for (int x = 0; x < route.size(); ++x) {
			for (int y = 0; y < route.get(x).size() - 1; ++y) {
				result = result + getDistanceInMiles(route.get(x).get(y), route.get(x).get(y + 1));
			}
		}
		return result;
	}
	
	/**
	 * Helper function to find the distance between two GeoPoints, using haversine formula built into Location class.
	 * Copied from http://stackoverflow.com/questions/5936912/how-to-find-the-distance-between-two-geopoints
	 * @param p1 the first point
	 * @param p2 the second point
	 * @return distance in miles
	 */
	private float getDistanceInMiles(GeoPoint p1, GeoPoint p2) {
	    double lat1 = ((double)p1.getLatitudeE6()) / 1e6;
	    double lng1 = ((double)p1.getLongitudeE6()) / 1e6;
	    double lat2 = ((double)p2.getLatitudeE6()) / 1e6;
	    double lng2 = ((double)p2.getLongitudeE6()) / 1e6;
	    float [] dist = new float[1];
	    Location.distanceBetween(lat1, lng1, lat2, lng2, dist);
	    return dist[0] * 0.000621371192f;
	}
	
    /**
     * Store center for state preservation.
     * @param center
     */
    public void storeCurrentMapCenter(GeoPoint center) {
    	mMapCenter = center;
    }
	
    /**
     * Returns the last 2 Points stored - used to give the RouteDrawView context from which to add arrows
     * onto the end of line segments when ACTION_UP MotionEvents occur (the user finishes tracing a segment).
     * @param projection Current map projection - to convert GeoPoints to current pixel coordinates.
     * @return first = second to last point, second = last point (both null if no stored points)
     */
    public Pair<Point, Point> getLastTwoDrawnPoints(Projection projection) {
    	
    	Pair<Point, Point> result; 
    	
    	if (mTouchPoints.size() > 0 && mTouchPoints.get(mTouchPoints.size() - 1).size() >= 2) {  		
    			
    		ArrayList<GeoPoint> lastArray = mTouchPoints.get(mTouchPoints.size() - 1);
    		Point first = projection.toPixels(lastArray.get(lastArray.size()-2), null);
    		Point second = projection.toPixels(lastArray.get(lastArray.size()-1), null);
    		result = new Pair<Point, Point>(first, second);
    	}
    	else {
    		result = new Pair<Point, Point>(null, null);
    	}
    	
    	return result;
    }
    
    /**
     * Returns last stored map center, or the user's current location if nothing was stored.
     * @param activity the Activity that wants the map center (typically the caller)
     * @return the last stored center of map
     */
    public GeoPoint getCurrentMapCenter(Activity activity) {
    	if (mMapCenter == null) {
			LocationManager locMan = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			Location lastSpot = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			mMapCenter = new GeoPoint(lastSpot);
    	}
    	return mMapCenter;
    }
    
    /**
     * This method snaps the currently measured points to nearby routes by downloading map information and
     * parsing it as XML, to find nearby "ways" (paths, roads, etc).
     * @param trace
     */
	public void snapTraceToWays(BoundingBoxE6 bbox, Context context, MapActivity activity) {
		// TODO
	}
	
	/**
	 * Called from the UI thread when the GetMapXMLAsyncTask finishes snapping the trace to any available ways.
	 * @param route the snapped route
	 */
	public void storeSnappedRoute(ArrayList<GeoPoint> route) {
		// TODO
	}
	
    public void storeZoomLevel(int zoom) {
    	mZoomLevel = zoom;
    }
    
    /**
     * 
     * @return 11 if no zoom level previously stored
     */
    public int getZoomLevel() {
    	return mZoomLevel;
    }
    
    public void setRouteDrawOpen(boolean mapstate) {
    	routeDrawOpen = mapstate;
    }
    
    /**
     * 
     * @return false if no map state previously stored
     */
    public boolean isRouteDrawOpen() {
    	return routeDrawOpen;
    }
    
    
	
	/******************************************/
	/******************************************/
	/*************  OLD STUFF  ****************/
	/******************************************/
	/******************************************/
	
	/*
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
	*/
} // end class
