package zones;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import simulation.Bot;
import simulation.Utilities;
import simulation.Vector;


public class Fire extends DangerZone implements Obstacle {

	private static final long serialVersionUID = 1L;
	private static final Color FireColor = new Color(255,140,0);
	private List<Point2D> ourVerticies;
	
	public Fire(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, FireColor);
		
		ourVerticies = Utilities.getVerticies(this);
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY/5;
	}	
	
	@Override
	public boolean hasOpenings() {
		return false;
	}

	@Override
	public Point2D getNearestOpening(Point2D p) {
		return null;
	}

	@Override
	public Vector getPathToNearestOpening(Point2D p) {
		return null;
	}

	@Override
	public Vector getPathAround(Vector intendedPath) {
		//get the closest vertex to the intended path
		Point2D closestVertex = ourVerticies.get(0);
		double closestDistanceSq = intendedPath.ptSegDistSq(closestVertex);
		
		for(Point2D v : ourVerticies) {
			double curDistanceSq = intendedPath.ptSegDistSq(v);
			if(curDistanceSq < closestDistanceSq) {
				closestDistanceSq = curDistanceSq;
				closestVertex = v;
			}
		}
		
		//make and return a vector from to that closest vertex
		//TODO possibly make this vector longer, or shift it away from the obstacle to avoid hitting the corner?
		return new Vector(intendedPath.getP1(), closestVertex);
	}

}
