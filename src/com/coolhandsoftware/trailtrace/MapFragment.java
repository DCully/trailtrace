
package com.coolhandsoftware.trailtrace;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.overlays.BasicInfoWindow;
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coolhandsoftware.topogen.R;

/**
 * This fragment holds the map itself for the MapActivity. 
 * It should only ever receive directions from the MapActivity - never the other way around.
 * 
 * @author David Cully david.a.cully@gmail.com
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
	private SnappablePolyline mPolyline;
	private SnappablePolyline.IPolylineDoubleTapReceiver mPolylineListener;
		
	/**
	 * Sets up and returns mMapView with its overlays. Sets parent activity as myMapView's 
	 * IMapViewLayoutListener, so the activity can update the map when it's ready to be drawn on.
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
		
		registerAsMapLayoutListener((MapActivity) getActivity());
		
		return myMapView;
	}
	
	/**
	 * Adds listener to MapView to receive callbacks when map layout changes. Used to delay drawing on map until it's laid out.			
	 * @param listener 
	 */
	public void registerAsMapLayoutListener(View.OnLayoutChangeListener listener) {
		myMapView.addOnLayoutChangeListener((MapActivity) getActivity());
	}
	
	/**
	 * Sets who will be added to receive callbacks from polylines when they are built.
	 * @param listener
	 */
	public void registerAsPolylineDtapListener(SnappablePolyline.IPolylineDoubleTapReceiver listener) {
		mPolylineListener = listener;
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
	 * This function clears whatever route the user had traced and then measured into the map.
	 */
	public void eraseTracedRoute() {
		if (mPolyline != null) {
			mPolyline.hideInfoWindow();
		}
		myMapView.getOverlays().remove(mPolyline);
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
	 * Used to store zoom level persistently in the MapTraceCoordinateManager along with the rest of the data.
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
	public void drawRoute(MeasuredRoute route) {
		eraseTracedRoute();
		mPolyline = new SnappablePolyline(getActivity(), mPolylineListener);
		ArrayList<GeoPoint> geoPointsList = combineGeoPointArrays(route.mPoints);
		mPolyline.setPoints(geoPointsList);
		mPolyline.getPaint().setColor(Color.BLUE);
		
		// multiplies by constant factor to compensate for smoothness of trace vs jaggedness of average trail
		// TODO this is not great - it needs to snap to routes. 

		
		// build an info window to show total distance
		DecimalFormat df = new DecimalFormat("##.##");
		df.setRoundingMode(RoundingMode.DOWN);
		mPolyline.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, myMapView));
		mPolyline.setTitle("Distance");
		mPolyline.setSnippet(df.format(route.mLength) + " miles");
		myMapView.getOverlays().add(mPolyline);
		mPolyline.getInfoWindow().open(mPolyline, geoPointsList.get(geoPointsList.size()-1), 0, 0);
	}
	
	/**
	 * Helper function to combine an array of arrays of GeoPoints to a single continuous array of GeoPoints.
	 * @param points array of arrays
	 * @return one array
	 */
	private ArrayList<GeoPoint> combineGeoPointArrays(ArrayList<ArrayList<GeoPoint>> points) {
		ArrayList<GeoPoint> result = new ArrayList<GeoPoint>();
		
		for (int x = 0; x < points.size(); ++x) {
			result.addAll(points.get(x));
		}
		
		return result;
	}
	
	/**
	 * Used to store the map center in model object onPause, etc.
	 * @return current map center
	 */
	public IGeoPoint getCurrentMapCenter() {
		return myMapView.getMapCenter();
	}
}
