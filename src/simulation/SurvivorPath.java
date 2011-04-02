package simulation;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import util.Utilities;
import util.shapes.LineSegment;
import zones.Zone;


public class SurvivorPath {

	//TODO remove "point" terminology from this class

	private static final long serialVersionUID = 269945074800423928L;
	private static final DecimalFormat df = new DecimalFormat("#.#####");

	private double pathLength;
	private Survivor sur;
	private ArrayList<BotInfo> pathWaypoints;
	private Point2D endPoint;
	private boolean complete;

	public SurvivorPath(Survivor _sur, List<BotInfo> _pathPoints, Point2D _endPoint, boolean _complete) {
		this(_sur, _pathPoints, _endPoint, -1.0, _complete);
	}

	public SurvivorPath(Survivor _sur, List<BotInfo> _pathPoints, Point2D _endPoint, double _pathLength, boolean _complete) {
		sur = _sur;
		complete = _complete;
		pathWaypoints = new ArrayList<BotInfo>();
		endPoint = _endPoint;

		//want to clone the individual points, to make sure we don't have any wrong pointers - need to do a loop
		pathWaypoints = new ArrayList<BotInfo>(_pathPoints);

		if(_pathLength < 0) {
			recalculatePathLength();
		} else {
			pathLength = _pathLength;
		}

	}

	public SurvivorPath(SurvivorPath _original) {
		this(_original.getSur(), _original.getPoints(), _original.getEndPoint(), _original.getPathLength(), _original.isComplete());
	}

	private void recalculatePathLength() {
		pathLength = 0.0;
		//start by adding up all the distances between bots
		BotInfo curWaypoint, prevWaypoint;
		prevWaypoint = pathWaypoints.get(0);
		if(pathWaypoints.size() > 1) {
			for(int i = 1; i < pathWaypoints.size(); i++) {
				curWaypoint = pathWaypoints.get(i);
				double sectionLength = curWaypoint.getCenterLocation().distance(prevWaypoint.getCenterLocation());
				//get the maximum multiplier - assume worst case
				double multiplier = curWaypoint.getZoneMultiplier() > prevWaypoint.getZoneMultiplier() ? curWaypoint.getZoneMultiplier() : prevWaypoint.getZoneMultiplier();

				pathLength += sectionLength * multiplier;

				prevWaypoint = curWaypoint;
			}
		}
		//add the distance from the last point to the end point
		pathLength += prevWaypoint.getZoneMultiplier() * prevWaypoint.getCenterLocation().distance(endPoint);

		//round to 5 decimal places
		pathLength = Double.parseDouble(df.format(pathLength));
	}

	public double getRealPathLength() {
		//TODO DEBUG ME!!! GETTING ANSWERS THAT ARE WAY TOO HIGH
		double realLength = 0.0;
		//add the distances from the points between bots
		Point2D curPoint, prevPoint;
		//make a list of our waypoints - don't care if they are the endpoint of the path or waypoints
		List<Point2D> justThePoints = new ArrayList<Point2D>(pathWaypoints.size() + 1);
		for(BotInfo waypts : pathWaypoints) {
			justThePoints.add(waypts.getCenterLocation());
		}
		justThePoints.add(endPoint);

//		System.out.println("There are " + justThePoints.size() + " points in the path");
		prevPoint = justThePoints.get(0);
		for(int i = 1; i < justThePoints.size(); i++) {
			curPoint = justThePoints.get(i);

			//create the line connecting the points
			LineSegment sectionLine = new LineSegment(curPoint, prevPoint);
			//figure out which zones it intersects
			List<? extends Shape> zoneIntersections = Utilities.findSegIntersectionInList(sectionLine, World.allZones.values());
//			System.out.println("Section intersects " + zoneIntersections.size() + " zones");
			if(zoneIntersections.size() == 0) {
				//this section is completely within a zone
				Zone encapsulatingZone = World.findZone(sectionLine.getP1());
				//add the correctly weighted length
				realLength += sectionLine.getLength() * encapsulatingZone.getPathWeightPerPixel();					
			} else {
				//for each zone this line intersects, find the segment that is inside that zone
				//and add that segment's weighted length to the total length
				for(Shape s : zoneIntersections) {
					if(s instanceof Zone) {
						Zone intersectingZone = (Zone) s;
						//get the intersection points that the line has with the zone
						//there should be a max of 2
						List<Point2D> intersectionPoints = new ArrayList<Point2D>(2);
						for(LineSegment side : intersectingZone.getSides()) {
							if(side.segmentsIntersect(sectionLine)) {
								intersectionPoints.add(side.intersectionPoint(sectionLine));
							}
						}
						//we know there will be at least 1, and a max of 2
						if(intersectionPoints.size() == 1) {
							//this section goes from one end to the edge of this zone.
							//figure out which endpoint is in this zone
							Point2D inZonePoint = intersectingZone.contains(sectionLine.getP1()) ? sectionLine.getP1() : sectionLine.getP2();
							//double check whichever point we got is inside the zone
							if(! intersectingZone.contains(inZonePoint)) {
								System.out.println("Said this point should be inside a zone, but it is not");
								System.out.println("Need to reevaluate logic here");
								System.exit(0);
							}
							//make a line for this part of the line
							LineSegment part = new LineSegment(inZonePoint, intersectionPoints.get(0));
							//add the weighted length of the part to the total
							realLength += part.getLength() * intersectingZone.getPathWeightPerPixel();
						} else {
							//there are exactly 2 points
							if(intersectionPoints.size() != 2) {
								System.out.println("Weird situation - got 3 intersection points. NON CONVEXLY SHAPED ZONE!!!!");
								System.exit(0);
							}
							//need to make a segment from each intersection point to the other
							//and add it's weighed length to the total
							LineSegment part = new LineSegment(intersectionPoints.get(0), intersectionPoints.get(1));
							realLength += part.getLength() * intersectingZone.getPathWeightPerPixel();
						}
					}
				}
			}
			prevPoint = curPoint;
		}

		return Double.parseDouble(df.format(realLength));
	}


	public double getPathLength() {
		return pathLength;
	}

	public Survivor getSur() {
		return sur;
	}

	/**
	 * @return the points
	 */
	public ArrayList<BotInfo> getPoints() {
		return pathWaypoints;
	}

	/**
	 * @return the endPoint
	 */
	public Point2D getEndPoint() {
		return endPoint;
	}

	public LineSegment getSurEndSegment() {
		return new LineSegment(pathWaypoints.get(0).getCenterLocation(), endPoint);
	}
	
	
	/**
	 * @return the complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 */
	public void setNowComplete() {
		this.complete = true;
	}

	public void addPoint(BotInfo pointToAdd) {
		if(this.isComplete()) {
			throw new IllegalAccessError("This path is complete - you cannot change it");
		}
		//		//first, adjust the path length down, removing the distance from the previous last waypoint to the endpoint
		//		BotInfo previousLast = pathWaypoints.get(pathWaypoints.size() - 1);
		//		pathLength -= previousLast.getCenterLocation().distance(endPoint);

		//add the new point to the path
		pathWaypoints.add(pointToAdd);
		recalculatePathLength();

		//		//add the distance from the previous last point to the new point, and from the new point to the end point
		//		pathLength += previousLast.getCenterLocation().distance(pointToAdd.getCenterLocation());
		//		pathLength += pointToAdd.getCenterLocation().distance(endPoint);
		//		
		//		//round it
		//		pathLength = Double.parseDouble(df.format(pathLength));
	}

	public double ptPathDist(Point2D pt) {
		return getNearestSegment(pt).ptSegDist(pt);
	}

	public LineSegment getNearestSegment(Point2D pt) {
		double minDist = Double.MAX_VALUE;
		LineSegment closestSeg = new LineSegment(pt, pt);

		LineSegment curSeg;
		double curSegDist;

		ArrayList<Point2D> allPoints = new ArrayList<Point2D>();

		for(BotInfo bi : pathWaypoints) {
			allPoints.add(bi.getCenterLocation());
		}

		allPoints.add(endPoint);

		for(int i = 0; i < allPoints.size()-1; i++) {
			curSeg = new LineSegment(allPoints.get(i), allPoints.get(i+1));

			curSegDist = curSeg.ptSegDist(pt);

			if(curSegDist < minDist) {
				minDist = curSegDist;
				closestSeg = curSeg;
			}
		}

		return closestSeg;	
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (complete ? 1231 : 1237);
		result = prime * result
		+ ((endPoint == null) ? 0 : endPoint.hashCode());
		long temp;
		temp = Double.doubleToLongBits(pathLength);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((pathWaypoints == null) ? 0 : pathWaypoints.hashCode());
		result = prime * result + ((sur == null) ? 0 : sur.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SurvivorPath))
			return false;
		SurvivorPath other = (SurvivorPath) obj;
		if (complete != other.complete)
			return false;
		if (endPoint == null) {
			if (other.endPoint != null)
				return false;
		} else if (!endPoint.equals(other.endPoint))
			return false;
		if (Double.doubleToLongBits(pathLength) != Double
				.doubleToLongBits(other.pathLength))
			return false;
		if (pathWaypoints == null) {
			if (other.pathWaypoints != null)
				return false;
		} else if (!pathWaypoints.equals(other.pathWaypoints))
			return false;
		if (sur == null) {
			if (other.sur != null)
				return false;
		} else if (!sur.equals(other.sur))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return sur + " : " + pathWaypoints + " : " + endPoint + " : length = " + pathLength + " : " + (complete ? " complete" : "NOT complete");
	}
}
