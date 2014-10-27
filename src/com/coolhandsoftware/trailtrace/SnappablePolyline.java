package com.coolhandsoftware.trailtrace;

import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import android.content.Context;
import android.view.MotionEvent;

public class SnappablePolyline extends Polyline {

	public interface IPolylineDoubleTapReceiver {
		public void onPolylineDoubletapped();
	}
	
	IPolylineDoubleTapReceiver mReceiver;
	
	public SnappablePolyline(Context ctx, IPolylineDoubleTapReceiver receiver) {
		super(ctx);
		mReceiver = receiver;
	}
	
	@Override public boolean onDoubleTap(final MotionEvent event, final MapView mapView) {
		
		Projection pj = mapView.getProjection();
		GeoPoint eventPos = (GeoPoint) pj.fromPixels((int) event.getX(), (int) event.getY());
		double tolerance = mPaint.getStrokeWidth();
		boolean tapWasOnLine = isCloseTo(eventPos, tolerance*3, mapView);
		if (tapWasOnLine){
			mReceiver.onPolylineDoubletapped();
			return true;
		}
		else {
			return false;
		}
	}

}
