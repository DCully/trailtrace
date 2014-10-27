package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import org.osmdroid.util.GeoPoint;

/**
 * Wrapper class for a list of GeoPoints and the length of the route they represent.
 * @author David Cully (david.a.cully@gmail.com)
 *
 */
public class MeasuredRoute {

	public ArrayList<ArrayList<GeoPoint>> mPoints;
	public float mLength;
	
	public MeasuredRoute(ArrayList<ArrayList<GeoPoint>> points, float distance) {
		mLength = distance;
		mPoints = points;
	}

}
