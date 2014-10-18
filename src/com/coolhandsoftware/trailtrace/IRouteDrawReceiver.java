package com.coolhandsoftware.trailtrace;

import java.util.ArrayList;

import android.util.Pair;


/**
 * This is the interface the RouteDrawView calls through when
 * it has finished rendering a drawn route from a touch event 
 * and needs to notify its parent Activity with its coordinates.
 * 
 * @author David Cully
 * david.a.cully@gmail.com
 * 
 */
public interface IRouteDrawReceiver {
	public abstract void storeXyCoordinates(ArrayList<Pair<Integer, Integer>> coordinates);
	public abstract void eraseButtonPressed();
	public abstract void measureButtonPressed();
}
