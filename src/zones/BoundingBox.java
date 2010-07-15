package zones;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import simulation.Utilities;
import simulation.Vector;

public class BoundingBox extends Rectangle2D.Double implements Obstacle {
	
	private static final long serialVersionUID = 1L;
	private List<Line2D> sides;

	public BoundingBox(double x, double y, double w, double h) {
		super(x, y, w, h);
		sides = Utilities.getSides(this);
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

	private final double IN_CORNER_THRESHOLD = 20.0;
	private final double IN_CORNER_THRESHOLD_SQ = IN_CORNER_THRESHOLD * IN_CORNER_THRESHOLD;
	
	@Override
	public Vector getPathAround(Vector intendedPath) {
		//essentially, since we can't get around it, we try to find a path that goes along the closest wall of the bounding box

		//first, see if this vector moves us outside the bounding box
		//if it doesn't we don't need to do anything just right now
		if(this.contains(intendedPath.getP2())) {
			return intendedPath;
		}
		
		//see which wall is nearest to the path start right now
		List<Line2D> closestSides = new ArrayList<Line2D>();
		closestSides.add(sides.get(0));
		double closestDistance = closestSides.get(0).ptSegDistSq(intendedPath.getP1());
		boolean inCorner = false;
		
		for(int i = 1; i < sides.size(); i++) {
			Line2D curSide = sides.get(i);
			double curDist = curSide.ptSegDistSq(intendedPath.getP1());
			
			//we're doing distance squared to save computation time, so we need to square the treshold
			if(Utilities.equalsWithin(curDist, closestDistance, IN_CORNER_THRESHOLD_SQ)) {
				//we're in a corner
				inCorner = true;
				closestSides.add(curSide);
			}
			
			if(curDist < closestDistance) {
				closestDistance = curDist;
				//only add the closest side if we aren't in a corner
				//if we are in a corner, it's already in there
				if(! inCorner) {
					closestSides.set(0, curSide);
				}
			}
		}
		
		//don't move if we're in a corner - return a vector of length 0
		if(inCorner) return new Vector(intendedPath.getP1(), intendedPath.getP1());
		
		//now that we know what side is closest, see if it's vertical or horizontal
		//we have a box, so it should be one or the other
		if(Utilities.isHorizontal(closestSides.get(0))) {
			//we have a horizontal side
			//that means, we want to go in the horizontal direction
			//meaning we want to return just the x component of the vector
			return (intendedPath.getXComponent());
		} else if(Utilities.isVertical(closestSides.get(0))) {
			//same logic, but want y component
			return (intendedPath.getYComponent());
		} else {
			//it says its not one or the other
			//FREAK OUT
			System.out.println("BONDING BOX SIDE IS NEITHER VERT NOR HORZ!!!");
			System.out.println("WRONG SIDE: " + new Vector(closestSides.get(0)));
			System.exit(0);
			return null;
		}
	}
	
	
}
