
package com.coolhandsoftware.trailtrace;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.overlays.BasicInfoWindow;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coolhandsoftware.topogen.R;

/**
 * This fragment holds the map itself for the MapActivity.
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class MapFragment extends Fragment { 

	/** view which holds the actual map tiles **/
	private MapView myMapView;
	
	/** used when building mMapView **/
	private ResourceProxy myResourceProxy;
	
	private CompassOverlay myCompassOverlay;
	private ScaleBarOverlay myScaleOverlay;
	private MyLocationNewOverlay myLocationOverlay;
	
	/** this is the object which is the user's measured trace, and is drawn on top of the map **/
	private Polyline mPolyline;
		
	/**
	 * Sets up and returns mMapView with its overlays. Sets parent activity as myMapView's 
	 * IMapViewLayoutListener, so the activity can update the map when it's ready to be 
	 * drawn on.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		myResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
		myMapView = new MapView(inflater.getContext(), 256, myResourceProxy);
		
		String[] hMapUrl = {"http://tile.thunderforest.com/landscape/"};
		final ITileSource hikingTileSource = new XYTileSource("hikingmap", null, 1, 18, 256, ".png", hMapUrl);
		myMapView.setTileSource(hikingTileSource); 
				
		myMapView.setBuiltInZoomControls(true);
		myMapView.setMultiTouchControls(true);
		
        final Context context = this.getActivity();
        myCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), myMapView);
		myMapView.getOverlays().add(myCompassOverlay);
        
		myScaleOverlay = new ScaleBarOverlay(context);
		myScaleOverlay.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.imperial);
		myMapView.getOverlays().add(myScaleOverlay);
		
		myLocationOverlay = new MyLocationNewOverlay(context, myMapView);
		myMapView.getOverlays().add(myLocationOverlay);
		
		myMapView.addOnLayoutChangeListener((MapActivity) getActivity());
				
		return myMapView;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		myCompassOverlay.disableCompass();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		myCompassOverlay.enableCompass();

	}
	
	/**
	 * Finds the distance in miles of a list of geopoints.
	 * @param pointsList
	 * @return distance in miles
	 */
	private float getDistanceInMiles(ArrayList<GeoPoint> pointsList) {
		float totalDistance = 0;
		
		for (int x = 1; x < pointsList.size(); ++x) {
			totalDistance += getDistanceInMiles(pointsList.get(x-1), pointsList.get(x));
		}
		
		return totalDistance;
	}
	
	/**
	 * This function clears whatever route the user had traced and then measured into the map.
	 */
	public void eraseTracedRoute() {
		if (mPolyline != null) {
			mPolyline.hideInfoWindow();
		}
		myMapView.getOverlays().remove(mPolyline); // so we first try to remove the last drawing
	}
	
	
	/**
	 * Helper function to find the distance between two GeoPoints.
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
	 * Centers map on coordinates (NOT E6 coordinates! This takes doubles...).
	 * @param lat Latitude
	 * @param lon Longitude
	 */
	public void moveMapToLocation(double lat, double lon) {
		myMapView.getController().setCenter(new GeoPoint(lat, lon));
	}
	
	/**
	 * 
	 * @return map's zoom level (1-18)
	 */
	public int getZoomLevel() {
		return myMapView.getZoomLevel();
	}
	
	/**
	 * Sets map's zoom level if within map's zoom range.
	 * @param zoom zoom level to set map to
	 */
	public void setZoomLevel(int zoom) {
		if (zoom > myMapView.getMinZoomLevel() && zoom < myMapView.getMaxZoomLevel()) {
			myMapView.getController().setZoom(zoom);
		}
	}
	
	/**
	 * Remember not to hold onto this projection - it goes out of date as soon as the map zoom level changes. 
	 * @return an up-to-date projection for converting to/from pixels and lat/lon.
	 */
	public Projection getCurrentProjection() {
		return myMapView.getProjection();
	}

	/**
	 * Redraws the hand-drawn route with new coordinates.
	 * @param geoPointsList the list of points on the route, in GeoPoints.
	 */
	public void drawRouteFromGeoPoints(ArrayList<GeoPoint> geoPointsList) {
		eraseTracedRoute();
		mPolyline = new Polyline(getActivity());
		
		mPolyline.setPoints(geoPointsList);
		mPolyline.getPaint().setColor(Color.BLUE);
		
		// multiplies by constant factor to compensate for smoothness of trace vs jaggedness of average trail
		// TODO this is not great - it needs to snap to routes. increases distance by 10%

		
		// build an info window to show total distance
		float totalDistance = getDistanceInMiles(geoPointsList);
		DecimalFormat df = new DecimalFormat("##.##");
		df.setRoundingMode(RoundingMode.DOWN);
		mPolyline.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, myMapView));
		mPolyline.setTitle("Distance");
		mPolyline.setSnippet(df.format(totalDistance*1.1) + " miles");
		myMapView.getOverlays().add(mPolyline);

		mPolyline.getInfoWindow().open(mPolyline, geoPointsList.get(geoPointsList.size()/2), 0, 0);
	}
	
	public IGeoPoint getCurrentMapCenter() {
		return myMapView.getMapCenter();
	}
}
