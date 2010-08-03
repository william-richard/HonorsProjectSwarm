package simulation;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;

import zones.Building;

public class Utilities {
	
	//finds all shapes in the shapeList that intersect the base shape
	public static List<? extends Shape> findAreaIntersectionsInList(Shape base, List<? extends Shape> shapeList) {
		//we're going to take advantage of Area's intersect method
		// so we need to turn base into an area		
		Area baseArea = new Area(base);
		
		//some Shapes are area-less like lines
		//need to deal with that case
		if(baseArea.isEmpty()) {
			//we have a line or a curve or something on our hands
			//make the line have a very small width so it has an area
			baseArea = new Area(giveArealessShapeArea(base));
		}

		//make the list of shapes that we'll end up returning
		List<Shape> intersectingShapes = new ArrayList<Shape>();

		//Then, we'll go through all the shapes in the list, and see if any of them intersect the base area
		for(Shape testShape : shapeList) {
			//make an area out of testShape
			Area testArea = new Area(testShape);
			//find the intersection
			testArea.intersect(baseArea);
			//now, test area is the area of intersection
			//see if that area is empty.
			//if it is not, we have an intersection and we should add it to the list
			if(! testArea.isEmpty()) {
				intersectingShapes.add(testShape);
			}
		}

		//we have found all the intersecting shape
		//return the list
		return intersectingShapes;
	}

	/*can figure out if the edges of these 2 intersect if
	 * 1) Their intersection (defined mathematically as the area shared by both of the shapes) is non empty.  If this is the case, they are not touching at all.
	 * 2) they intersection is not equal to the total area of either of the shapes.  In this case, one is completely within the other.
	 */
	public static boolean edgeIntersects(Shape s1, Shape s2) {
		//handle the cases for lines
		if(s1 instanceof Line2D && s2 instanceof Line2D) {
			return ((Line2D) s1).intersectsLine((Line2D)s2);
		} else if(s1 instanceof Line2D) {
			return lineIntersectsShape((Line2D) s1, s2);
		} else if(s2 instanceof Line2D) {
			return lineIntersectsShape((Line2D) s2, s1);
		}
		
		//possible lines are handled - we only have real shapes (unless we're passed curves, but we shouldn't be)
		Area a1 = new Area(s1);
		Area a2 = new Area(s2);
		
		if(a1.isEmpty() || a2.isEmpty()) {
			System.out.println("GOT AN AREALESS SHAPE FOR EDGE INTERSECTIONS!!!");
			System.out.println(s1);
			System.out.println(s2);
			System.exit(0);
		}

		Area intersection = new Area(s1);
		intersection.intersect(a2);

		if(intersection.isEmpty()) return false;
		if(intersection.equals(a1) || intersection.equals(a2)) return false;

		return true;
	}
	
	public static boolean lineIntersectsShape(Line2D l, Shape s) {
		//get all the edges of the shape
		List<Line2D> edges = getSides(s);
		
		for(int i = 0; i < edges.size(); i++) {
			Line2D curEdge = edges.get(i);
			if(curEdge.intersectsLine(l)) return true;
		}
		
		return false;
	}
	
	
	public static Polygon giveArealessShapeArea(Shape s) {
		//get all the verticies of the shape
		List<Point2D> shapeVerticies = getVerticies(s);
		
		//now, make a polygon with those verticies
		Polygon sWithArea = new Polygon();
		Point2D curPoint;
		for(int i = 0; i < shapeVerticies.size(); i++) {
			curPoint = shapeVerticies.get(i);
			//Polygon only takes ints, so this is a VERY rough estimate
			sWithArea.addPoint((int)curPoint.getX(), (int)curPoint.getY());
		}
		
		//now, go through again and add the sames points again, but offset by a bit
		//this way, we'll get the polygon to have some area
		for(int i = shapeVerticies.size()-1; i >= 0; i--) {
			curPoint = shapeVerticies.get(i);
			sWithArea.addPoint((int)curPoint.getX() + 1, (int) curPoint.getY() + 1);
		}
		
		return sWithArea;
	}
	
	
	/*
	 * ASSUMING THAT ALL PASSED SHAPES ARE CONTIGUOUS AND DO NOT HAVE CURVES
	 * IF THEY DO, THIS METHOD WILL FAIL
	 */
	public static List<Point2D> getVerticies(Shape s) {
		//get all the sides
		List<Line2D> sides = getSides(s);
		
		//get all the points from the sides
		List<Point2D> verticies = new ArrayList<Point2D>();
		Line2D prevLine = null;
		for(Line2D l : sides) {
			verticies.add(l.getP1());
			//P2 *SHOULD* be the P1 of the next line
			//check
			if(prevLine != null) {
				if(! prevLine.getP2().equals(l.getP1())) {
					//FLIP OUT
					System.out.println("AHHHHHH lines don't match up!!!!");
				}
			}
			prevLine = l;
		}

		return verticies;

	}
	
	public static List<Line2D> getSides(Shape s) {
		//get s's PathIterator
		FlatteningPathIterator fpi = new FlatteningPathIterator(s.getPathIterator(null), .1);
		
		//now, go through the iterator and extract the sides
		double[] curCoords = new double[6];
		List<Line2D> sides = new ArrayList<Line2D>();
		
		Point2D moveToPoint = new Point2D.Double();
		Point2D lastPoint = new Point2D.Double();
		
		while(! fpi.isDone()) {
			int segType = fpi.currentSegment(curCoords);

			Point2D nextPoint = new Point2D.Double(curCoords[0], curCoords[1]);
			
			//shouldn't need to deal with QUAD_TO or CUBIC_TO because this is flattened
			if(segType == PathIterator.SEG_MOVETO) {
				//store the moveto point
				moveToPoint = (Point2D) nextPoint.clone();
			} else if(segType == PathIterator.SEG_LINETO) {
				//store a line from the last point to the point we just got
				sides.add(new Line2D.Double(lastPoint, nextPoint));
			} else if(segType == PathIterator.SEG_CLOSE) {
				//store a line from the last point to the last point we moved to
				sides.add(new Line2D.Double(lastPoint, moveToPoint));
			} else if(segType == PathIterator.SEG_CUBICTO || segType == PathIterator.SEG_QUADTO) {
				System.out.println("GOT A CURVE WHEN GETTING SIDES!!!! SHOULD NOT HAPPEN!!!");
			}
			
			lastPoint = (Point2D) nextPoint.clone();
			fpi.next();
		}
		
		return sides;
	}
	
	public static Point2D getNearestPoint(Shape onThisShape, Point2D toThisPoint) {
//		if(onThisShape.contains(toThisPoint)) {
//			return toThisPoint;
//		}
		
		
		//get the closest side
		Line2D closestSide = getNearestSide(onThisShape, toThisPoint);
		
		//now that we know the closest side, find the nearest point on that side
		
		//first, check the endpoints
		//can do this with a dot product
		if( ((new Vector(closestSide.getP1(), closestSide.getP2())).dot(new Vector(closestSide.getP2(), toThisPoint))) > 0) {
			return closestSide.getP2();
		}
		
		if( ((new Vector(closestSide.getP2(), closestSide.getP1())).dot(new Vector(closestSide.getP1(), toThisPoint))) > 0) {
			return closestSide.getP1();
		}
		
		//it isn't the end points, so find the point on the line that applies
		//make a vector out of the closest side
		Vector closestVector = new Vector(closestSide);
		
		//also, make a vector from the closest side's starting point to our point
		Vector vectToThisPoint = new Vector(closestVector.getP1(), toThisPoint);
		
		//get the scalar projection of <vectToThisPoint> onto <closestVector>
		double distToPointONClosestSide = vectToThisPoint.scalerProjectionOnto(closestVector);
		
		//rescale the closest side to that length, so now it's end point will be the closest point
		closestVector = closestVector.rescale(distToPointONClosestSide);
		
		//return that closest point we just found
		return closestVector.getP2();
	}
	
	public static Line2D getNearestSide(Shape onThisShape, Point2D toThisPoint) {
		//get all the sides of this shape
		List<Line2D> sides = getSides(onThisShape);
		
		//figure out which side is closest to us
		Line2D closestSide = sides.get(0);
		double closestDistanceSquared = closestSide.ptSegDistSq(toThisPoint);
		
		for(Line2D l : sides) {
			double curDistanceSquared = closestSide.ptSegDistSq(toThisPoint);
			if(curDistanceSquared < closestDistanceSquared) {
				closestDistanceSquared = curDistanceSquared;
				closestSide = l;
			}
		}

		//now that we have the side, return it
		return closestSide;
	}
	
	public static double getAngleBetween(Point2D thisPoint, Point2D andThisOtherPoint, Point2D fromThisPoint) {
		Vector v1 = new Vector(fromThisPoint, thisPoint);
		Vector v2 = new Vector(fromThisPoint, andThisOtherPoint);
		return v1.getAngleBetween(v2);
	}
	
	public static List<Point2D> getDiscontinuityPoints(Shape base, Shape outsider) {
		//if the outsider is a building, we need to consider it's floorplan
		if(outsider instanceof Building) {
			outsider = ((Building) outsider).getFloorplan();
		}
		
		//first, get the intersection of the two shapes
		Area baseArea = new Area(base);
		Area outsiderArea = new Area(outsider);
		
		Area intersectionArea = (Area) outsiderArea.clone();
		intersectionArea.intersect(baseArea);
		
		//get the vertices of the intersection area
		List<Point2D> intersectionVerticies = getVerticies(intersectionArea);
		
		//now, try to find the combination of points that maximizes the angle between them from the center of the base
		Point2D baseCenterPoint = new Point2D.Double(base.getBounds2D().getCenterX(), base.getBounds2D().getCenterY());
		
		List<Point2D> discontinuityPoints = new ArrayList<Point2D>();
		double maxAngleBetween = 0.0;
		
		for(int i = 0; i < intersectionVerticies.size(); i++ ) {
			Point2D outsidePoint = intersectionVerticies.get(i);
			for(int j = i; j < intersectionVerticies.size(); j++) {
				Point2D insidePoint = intersectionVerticies.get(j);
				
				double curAngleBetween = Math.abs(getAngleBetween(outsidePoint, insidePoint, baseCenterPoint));
				
				if(curAngleBetween > maxAngleBetween) {
					maxAngleBetween = curAngleBetween;
					if(discontinuityPoints.size() == 0) {
						//need to fill the list the first time through
						discontinuityPoints.add(outsidePoint);
						discontinuityPoints.add(insidePoint);
					} else if (discontinuityPoints.size() > 2){
						System.out.println("MORE THAN 2 DISCONTINUITY POINTS FOR ONE SHAPE! THIS IS IMPOSSIBLE!!!");
						System.exit(0);
					} else {
						discontinuityPoints.set(0, outsidePoint);
						discontinuityPoints.set(1, insidePoint);
					}
				}
			}
		}
		
		return discontinuityPoints;
	}

	public static boolean isHorizontal(Line2D l) {
		return l.getY1() == l.getY2();
	}
	
	public static boolean isVertical(Line2D l) {
		return l.getX1() == l.getX2();
	}
	
	public static boolean equalsWithin(double d1, double d2, double epsilon) {
		double diff = d1 - d2;
		//make sure it's positive
		if(diff < 0.0) diff *= -1.0;
		return diff <= epsilon;
	}
	
	public static Point2D getIntersectionPoint(Line2D l1, Line2D l2) {
		//need to do the math
		if(l1 == null) System.out.println("l1 is null");
		if(l2 == null) System.out.println("l2 is null");
		
		//make sure the segements intersect
		if(! l1.intersectsLine(l2)) return null;
				
		//it is possible we will get passed a line of length 0, where the start point and end point are the same
		//if this happens, return one of the points, as we have already determined that the other line intersects that point
		if(l1.getP1().equals(l1.getP2())) return l1.getP1();
		if(l2.getP1().equals(l2.getP2())) return l2.getP2();
		
//		System.out.println("Trying to find intersection between '" + Utilities.lineToString(l1) + "' and '" + Utilities.lineToString(l2) + "'");
		
//		//first, need to handle vertcial and horizontal lines
//		//if either of the lines are horizontal, just get the point on the other line at the first line's x value
//		if(Utilities.isHorizontal(l1)) {
//			
//		}
//		
//		
//		
//		
//		//according to wikipedia, the following formula solves for the x intersection point and the y intersection point
//		//if l1 has points x1,y1 and x2,y2 and l2 has points x3,y3 and x4,y4 then
//		//intersection x = ((x1y2-y1x2)(x3-x4) - (x1-x2)(x3y4-y3x4))/((x1-x2)(y3-y4)-(y1-y2)(x3-x4))
//		//intersection y = ((x1y2-y1x2)(y3-y4) - (y1-y2)(x3y4-y3x4))/((x1-x2)(y3-y4)-(y1-y2)(x3-x4)
//		//so lets do it
//		double intersectionX = ((l1.getX1()*l1.getY2() - l1.getY1()*l1.getX2())*(l2.getX1() - l2.getX2()) - (l1.getX1() - l1.getX2())*(l2.getX1()*l2.getY2()-l2.getY1()*l2.getX2()))
//								/ ((l1.getX1()-l1.getX2())*(l2.getY1()-l2.getY2())-(l1.getY1()-l1.getY2())*(l2.getX1()-l2.getX2()));
//		double intersectionY = ((l1.getX1()*l1.getY2() - l1.getY1()*l1.getX2())*(l2.getY1() - l2.getY2()) - (l1.getY1() - l1.getY2())*(l2.getX1()*l2.getY2()-l2.getY1()*l2.getX2()))
//								/ ((l1.getX1()-l1.getX2())*(l2.getY1()-l2.getY2())-(l1.getY1()-l1.getY2())*(l2.getX1()-l2.getX2()));
//		Point2D intersectionPoint = new Point2D.Double(intersectionX, intersectionY);
//		
//		System.out.println("Found the intersection is " + Utilities.pointToString(intersectionPoint));
//		
//		return intersectionPoint;
		
		
		
		
		//also need to handle the case of the vertical line i.e. the X1 and X2 are equal
		//again, we have already determined that the lines inersect at some point,
		//so if line A is vertical, then we just need to find the point on line B that has the x value of line A
		
		//firstly, if both lines are vertical, then since they intersect, there will be at least 1 intersection point, and possibly more
		//this probably won't happen, and if it does, we'll find an end point on one line that is on the other line and return it
		//going to determine if a point lines on the line by using the ptLineDist method
		if(Utilities.isVertical(l1) && Utilities.isVertical(l2)) {
			//test the endpoints of l1
			if(l2.ptLineDist(l1.getP1()) == 0.0) {
				return l1.getP1();
			} else if (l2.ptLineDist(l1.getP2()) == 0.0){
				return l1.getP2();
			} else {
				//it is possible that l1 totally encompasses l2
				//in that case, neither of the other situations will get tripped
				//so if we get here, we know that all points on l2 lie on l1
				//so just return one of l2's endpoints
				return l2.getP1();
			}
		}
		
		//now handle the case if only one line is vertical
		//we can safely assume that the other line is not vertical
		if(Utilities.isVertical(l1)) {
			//evaluate l2 to find the y value on it with the x value of l1
			double m2 = (l2.getY1() - l2.getY2()) / (l2.getX1() - l2.getX2());
			double intersectY = m2*(l1.getX1() - l2.getX1()) + l2.getY1();
			return new Point2D.Double(l1.getX1(), intersectY);
		}
		
		if(Utilities.isVertical(l2)) {
			double m1 = (l1.getY1() - l1.getY2()) / (l1.getX1() - l1.getX2());
			double intersectY = m1*(l2.getX1() - l1.getX1()) + l1.getY1();
			return new Point2D.Double(l2.getX1(), intersectY);
		}
		
		//lets calculate the slopes (m) of the lines
		double m1 = (l1.getY1() - l1.getY2()) / (l1.getX1() - l1.getX2());
		double m2 = (l2.getY1() - l2.getY2()) / (l2.getX1() - l2.getX2());		
		
		//now, I did the algebra and the x value of the intersection point should be the following
		double intersectX = (m2*l2.getX1() - l2.getY1() - m1*l1.getX1() + l1.getY1()) / (m2-m1);
		//and just using regular point slope form, and solving for y we find that the intersection point's y coordinate is the following
		double intersectY = m1*intersectX - m1*l1.getX1() + l1.getY1();
		
		return new Point2D.Double(intersectX, intersectY);	
	}
	
	public static String pointToString(Point2D p) {
		if(p == null) return null;
		return p.getX() + ", " + p.getY();
	}
	
	public static String lineToString(Line2D l) {
		if(l== null) return null;
		return pointToString(l.getP1()) + " --> " + pointToString(l.getP2());
	}
	
}
