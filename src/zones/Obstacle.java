package zones;

import java.awt.geom.Point2D;

import simulation.Vector;


public interface Obstacle {
	
	public boolean hasOpenings();
	
	public Point2D getNearestOpening(Point2D p);
	
	public Vector getPathToNearestOpening(Point2D p);

	public Vector getPathAround(Vector intendedPath);
	
}
