package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.coolhandsoftware.topogen.R;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;

/**
 * This fragment allows the user to trace a raw route on the screen. It reports coordinates traced
 * to whomever is registered as its receiver via the IRouteDrawReceiver interface.
 * 
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class RouteDrawFragment extends Fragment {

	/** the view which allows for tracing **/
	private RouteDrawView mRouteDrawView;
	
	/** the recipient of callbacks **/
	private IRouteDrawReceiver mReceiver;
	
	/**
	 * Stores reference to view after inflating but before returning.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) 
	{
		View layout = inflater.inflate(R.layout.route_draw_fragment, container, false);
		mRouteDrawView = (RouteDrawView) layout.findViewById(R.id.route_draw_view);
		return layout;
	}
	
	/**
	 * Draws route onto the screen from pairs of pixel coordinates. Used to refresh trace state from elsewhere.
	 * @param storedLineSegments the list of list of points to be stored - each inner list is a segment
	 */
	public void drawRouteFromPixels(ArrayList<ArrayList<Pair<Integer, Integer>>> storedLineSegments) {

		mRouteDrawView.clearTracedRoute();
		
		for (int segment = 0; segment < storedLineSegments.size(); ++segment) {
			ArrayList<Pair<Integer, Integer>> curSegment = storedLineSegments.get(segment);
			mRouteDrawView.moveLineTo(curSegment.get(0).first, curSegment.get(0).second); // move the paintbrush to the first point
						
			for (int point = 1; point < curSegment.size(); ++ point) {
				mRouteDrawView.drawLineTo(curSegment.get(point).first, curSegment.get(point).second); // draw the line to the next point
			}
			
			mRouteDrawView.drawArrowAtEndOf(curSegment);
		}
	}
	
	/**
	 * Erases what's drawn on the screen.
	 */
	public void clearTracedRoute() {
		mRouteDrawView.clearTracedRoute();
	}
	
	/**
	 * Allows caller to register to receive updates from RouteDrawView upon draw events via an interface.
	 * @param receiver the object implementing the IRouteDrawReceiver interface
	 */
	public void registerAsRouteDrawReceiver(IRouteDrawReceiver receiver) {
		
		mReceiver = receiver;
		
		View aView = getView().findViewById(R.id.route_draw_view); // get a reference to the correct view
		if (aView == null) {
			throw new NoSuchElementException("Couldn't find route draw view inside RouteDrawFragment"); 
			// if the view doesn't exist, throw an exception
		}
		else {
			// store a reference to the route draw view and cast it as such
			mRouteDrawView = (RouteDrawView) aView;
			mRouteDrawView.initializeMemberVariables(mReceiver);
		}
		
		// set up this fragment as the receiver of all button presses, too
		ImageButton eraseButton = (ImageButton) getView().findViewById(R.id.eraser_button);
		ImageButton measureButton = (ImageButton) getView().findViewById(R.id.measure_button);
		
		// the erase button should call clearTracedRoute to clear the screen
		eraseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mReceiver.eraseButtonPressed();
				mRouteDrawView.clearTracedRoute();
			}
		});
		
		// the measure button should crunch the numbers on all the coordinates collected
		measureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mReceiver.measureButtonPressed();
				mRouteDrawView.clearTracedRoute();
			}
		});	
	}
	
	/**
	 * Determines size for arrow based on activity's size on the screen.
	 */
	@SuppressWarnings("deprecation") // its ok, its in a conditional
	@Override
	public void onResume() {
		super.onResume();
		int Measuredwidth = 0;
		int Measuredheight = 0;
		Point size = new Point();
		WindowManager w = getActivity().getWindowManager();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
		{
		    w.getDefaultDisplay().getSize(size); // Lint check about API disabled - covered with conditional

		    Measuredwidth = size.x;
		    Measuredheight = size.y;
		}
		else
		{
		    Display d = w.getDefaultDisplay();
		    Measuredwidth = d.getWidth();
		    Measuredheight = d.getHeight();
		}
		mRouteDrawView.arrowSize = Measuredwidth*Measuredheight/100000;
	}
} // end RouteDrawFragment
