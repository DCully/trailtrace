package com.coolhandsoftware.trailtrace;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

/**
 * This View is how the MapActivity captures touch events when the user 
 * is drawing on the map. It's fragment is transacted above the map when
 * the tracing feature is enabled. It reports the list of traced pairs to 
 * its receiver (set in ctor) when it registers that a trace has been 
 * completed (when the user lifts the finger).
 * 
 * @author David Cully
 * 
 */
public class RouteDrawView extends View {
	
	/** holds the color/style info for our trace, as set in ctor **/
	private Paint mPaint = new Paint();
	
	/** the path object we pass with our paint object to drawPath **/
	private Path mPath = new Path();    
	
	/** whatever got passed in the ctor - will receive x,y coordinate reports **/
	private IRouteDrawReceiver mReceiver;
	
	/** size of arrows put on end of each segment after user traces them - re-calculated in onResume from activity size **/
	public int arrowSize = 25;
	
	/**
	 * This is the interface the RouteDrawView calls through when
	 * it has finished rendering a drawn route from a touch event 
	 * and needs to notify its parent Activity with its coordinates.
	 */
	public interface IRouteDrawReceiver {
		public abstract void storePixelPoint(Point coordinate, boolean isNewSegment);
		public abstract void onEraseButtonPressed();
		public abstract void onMeasureButtonPressed();
	}
	
	
	/**
	 * This ctor may be called from the XML, and calls thru to the superclass.
	 * Remember to always call initializeMemberVariables(receiver) on this View!
	 * @param context where this was called from
	 */
	public RouteDrawView(Context context) {
		super(context);;
	}
	
	/**
	 * This ctor may be called from the XML, and calls thru to the superclass.
	 * Remember to always call initializeMemberVariables(receiver) on this View!
	 * @param context where this was called from
	 * @param attrs attribute set passed in, which contains the address of this object's receiver
	 */
	public RouteDrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/**
	 * This ctor may be called from the XML, and calls thru to the superclass.
	 * Remember to always call initializeMemberVariables(receiver) on this View!
	 * @param context where this was called from
	 * @param attrs attribute set passed in, which contains the address of this object's receiver
	 * @param defStyle overriden style for this view
	 */
	public RouteDrawView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/**
	 * Register to receive callbacks when the user traces a route.
	 * @param receiver
	 */
	public void registerAsReceiver(RouteDrawView.IRouteDrawReceiver receiver) {
		mReceiver = receiver;
		mPaint.setStrokeWidth(6f);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
	}
	
	/**
	 * this is called when the view is being rendered to the screen; it draws our path
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPath(mPath, mPaint);
	}
	
	/**
	 * Allows the RouteDrawView to handle touch events, for drawing
	 * the traced routes on the screen and reporting them to the parent 
	 * activity when the tracing is completed (for MapView handling).
	 * 
	 * Lint drags up a warning about how I need to override performClick()
	 * if I use it in this function, but I don't use that function in here,
	 * so I disabled that specific check.
	 */	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// called when the device registers a single touch event on this view
		int x = Math.round(event.getX());
		int y = Math.round(event.getY());
						
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN: 
			mPath.moveTo(x, y);
			invalidate();
			mReceiver.storePixelPoint(new Point(x, y), true);
			return true;
		case MotionEvent.ACTION_MOVE:		
			mPath.lineTo(x, y);
			invalidate();
			mReceiver.storePixelPoint(new Point(x, y), false);
			return true;
		case MotionEvent.ACTION_UP:
			invalidate();
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Draws an arrow 30 degrees off the line segment defined by the two points in the pair.
	 * The tip of the arrow is the second point in the pair.
	 * 
	 * TODO this doesn't work - it breaks my polyline somehow
	 * 
	 * @param mTracedXyCoordinates the list of points that makes up the segment (in pixels).
	 */
	public void drawArrowAtEndOf(Pair<Point, Point> points) {
		
		/**
		 * We draw two segments out from the tip of the arrow back and 30 degrees out from where the 
		 * point before the tip of the arrow is. 
		 */
		
		//Pair<Integer, Integer> tipOfArrow = mTracedXyCoordinates.get(mTracedXyCoordinates.size()-1);
		//Pair<Integer, Integer> oneBeforeTip = mTracedXyCoordinates.get(mTracedXyCoordinates.size()-4);
		
		// slope at the middle of the trace
		double slope = ((double) points.second.y - (double) points.first.y) / ((double) points.second.x - (double) points.first.x);
		
		if (Double.isInfinite(slope)) {
			slope = 100000;
		}
		
		// find the point behind the midpt to find our arrow segment endings from
		Pair<Double, Double> possibleXVals = findPossibleXOnLine(arrowSize, slope, points.second.x, points.second.y);
		double orthoPtX;
		
		// adjust for the direction the arrow should point in - there's four directions, so four cases
		if (points.second.x >= points.first.x && points.second.y >= points.first.y) {
			// if both x and y of the arrow's tip are bigger than the point before it, down and to right
			orthoPtX = possibleXVals.second;
		}
		else if (points.second.x >= points.first.x && points.second.y < points.first.y) {
			// if tipofarrow's x is bigger, but y is smaller, pointing up and to right
			orthoPtX = possibleXVals.second;
		}
		else if (points.second.x < points.first.x && points.second.y >= points.first.y) {
			// if tipofarrow's x is smaller but y is bigger, pointing down and to left
			orthoPtX = possibleXVals.first;
		}
		else {
			// pointing up and to left
			orthoPtX = possibleXVals.first;
		}
		double orthoPtY = findYFromXOnLine(slope, orthoPtX, points.second.x, points.second.y);
		
		// slope to use with orthoPt - already corrected it if slope = infinity, now correct if slope = 0
		double invslope;
		if (slope < 0.1 && slope > -0.1) {
			invslope = 1000000;
		}
		else {
			invslope = -1/slope; // normal case - slope isn't 0 or infinity, so we can just invert it
		}
		
		// find x-vals and y-vals of points arrowSize/2 (for a 30* angled arrow) away from orthoPt on line with slope=invslope 
		Pair<Double, Double> xVals = findPossibleXOnLine(arrowSize/2, invslope, orthoPtX, orthoPtY);
		Pair<Double, Double> yVals = findYValsFromXVals(xVals, invslope, orthoPtX, orthoPtY);
				
		// draw the arrow's line segments
		mPath.moveTo(points.second.x, points.second.y);
		mPath.lineTo(xVals.first.floatValue(), yVals.first.floatValue());
		mPath.moveTo(points.second.x, points.second.y);
		mPath.lineTo(xVals.second.floatValue(), yVals.second.floatValue());
		invalidate();
		
	}
	
	/**
	 * This function finds the two possible values of X that are a given distance away from a given point
	 * with a given slope of the line.
	 * 
	 * The first X-value in the pair is the point on the line with x less than the given point.
	 * The second X-value in the pair is the point on the line with x greater than the given point.
	 * 
	 * @param distance the distance from the given point to our two other points
	 * @param slope the slope of the line these points are all on
	 * @param X the X-value of the initial (middle) point
	 * @param Y the Y-value of the initial (middle) point
	 * @return a pair of X-values for the two points, as doubles.
	 */
	private Pair<Double, Double> findPossibleXOnLine(double distance, double slope, double X, double Y) {
		// uses a system of two equations formed from the distance formula and the point-slope equation of a line
		// the two equations resolve to a polynomial which yields two solutions via the quadratic formula
		
		double a = 1;
		double b = -2*X;
		double c = X*X - (distance*distance)/(1+slope*slope);
		
		// check determinant for error?
		double first = (-b + Math.sqrt(b*b-4*a*c))/(2*a);
		double second = (-b - Math.sqrt(b*b-4*a*c))/(2*a);
		
		Pair<Double, Double> result = new Pair<Double, Double>(first,second);
		return result;
	}
	
	/**
	 * Finds the Y-values of two points on a line from their X-coordinates.
	 * @param xVals the x-coordinates of the two points
	 * @param slope the slope of the line
	 * @param X the x-coordinate of some other point on the line
	 * @param Y the y-coordinate of some other point on the line
	 * @return a pair of y-values - the first corresponds to the first x-value in xVals, the second two the second
	 */
	private Pair<Double, Double> findYValsFromXVals(Pair<Double, Double> xVals, double slope, double X, double Y) {
		double first = findYFromXOnLine(slope, xVals.first, X, Y);
		double second = findYFromXOnLine(slope, xVals.second, X, Y);
		Pair<Double, Double> result = new Pair<Double, Double>(first, second);
		return result;
	}
	
	/**
	 * Point slope formula to find Y from X
	 * @param slope slope of the line
	 * @param unmatchedX X coordinate you want a Y coordinate for
	 * @param X x-val of a point on the line
	 * @param Y y-val of a point on the line
	 * @return the Y value to match unmatchedX
	 */
	private double findYFromXOnLine(double slope, double unmatchedX, double X, double Y) {
		return slope*(unmatchedX - X) + Y;
	}

	/**
	 * Allows you to directly erase the route that's been drawn on the map.
	 */
	public void clearTracedRoute() {
		mPath.reset();
		//System.out.println("clearing traced route");
		invalidate();
	}
	
	/**
	 * Move the "tip of the paintbrush" of the route drawing on the map.
	 * @param x x-value of pixel coordinate
	 * @param y y-value of pixel coordinate
	 */
	public void moveLineTo(float x, float y) {
		mPath.moveTo(x,y);
		//System.out.println("moved line to " + x + ", " + y);
		invalidate();
	}
	
	
	/**
	 * Draw the line on the map from wherever it currently is, to these parameterized coordinates.
	 * @param x x-value of pixel coordinate
	 * @param y y-value of pixel coordinate
	 */
	public void drawLineTo(float x, float y) {
		mPath.lineTo(x, y);
		//System.out.println("drew line to " + x + ", " + y);
		invalidate();
	}
}
