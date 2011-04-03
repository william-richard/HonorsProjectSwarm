package util;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import util.shapes.Circle2D;
import util.shapes.LineSegment;
import zones.Zone;

public class Utilities {

	public final static double SMALL_EPSILON = .00001;

	//finds all shapes in the shapeList that intersect the base shape
	public static List<? extends Shape> findAreaIntersectionsInList(Shape base, Collection<? extends Shape> shapeList) {		
		//see if the base is a circle
		//if it is, take a shortcut
		if(base instanceof Circle2D) {
			return findAreaIntersectionInList((Circle2D) base, shapeList);
		}

		//we're going to take advantage of Area's intersect method
		// so we need to turn base into an area		
		Area baseArea = new Area(base);

		//some Shapes are area-less like lines
		//need to deal with that case
		if(baseArea.isEmpty()) {
			throw new IllegalArgumentException("Base shape doesn't have an area");
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

	public static List<? extends Shape> findAreaIntersectionInList(Circle2D base, Collection<? extends Shape> shapeList) {
		//make sure the base has an area
		if(Utilities.shouldEqualsZero(base.getRadius())) {
			throw new IllegalArgumentException("Base circle doesn't have any area");
		}

		//test if each shape intesects the circle
		//keep a list of the shapes that do
		List<Shape> intersectingShapes = new ArrayList<Shape>();
		for(Shape curShape : shapeList) {
			//test if each shape intersects the base
			if(base.intesectsShape(curShape)) {
				intersectingShapes.add(curShape);
			}
		}

		return intersectingShapes;
	}
	
	public static List<? extends Shape> findSegIntersectionInList(LineSegment line, Collection<? extends Shape> shapeList) {
		ArrayList<Shape> resultList = new ArrayList<Shape>();
		
		for(Shape s : shapeList) {
			//get all of this shape's sides
			List<LineSegment> shapeSides = Utilities.getSides(s);
			for(LineSegment curSide : shapeSides) {
				if(curSide.segmentsIntersect(line)) {
					resultList.add(s);
					break;
				}
			}
		}
		
		return resultList;
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
		List<LineSegment> edges = getSides(s);

		for(int i = 0; i < edges.size(); i++) {
			Line2D curEdge = edges.get(i);
			if(curEdge.intersectsLine(l)) return true;
		}

		return false;
	}

	/*
	 * ASSUMING THAT ALL PASSED SHAPES ARE CONTIGUOUS AND DO NOT HAVE CURVES
	 * IF THEY DO, THIS METHOD WILL FAIL
	 */
	public static List<Point> getVerticies(Shape s) {
		//get all the sides
		List<LineSegment> sides = getSides(s);

		//get all the points from the sides
		List<Point> verticies = new ArrayList<Point>();
		Line2D prevLine = null;
		for(Line2D l : sides) {
			verticies.add(new Point((int)l.getX1(), (int)l.getY1()));
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

	public static List<LineSegment> getSides(Shape s) {
		return Utilities.getSides(s, false);
	}

	public static List<LineSegment> getSides(Shape s, boolean overrideZone) {
		//first, see if it is a zone and if we have pre-computed the sides
		if(s instanceof Zone && !overrideZone) {
			return ((Zone)s).getSides();
		}

		//get s's PathIterator
		FlatteningPathIterator fpi = new FlatteningPathIterator(s.getPathIterator(null), .01);

		//now, go through the iterator and extract the sides
		double[] curCoords = new double[6];
		List<LineSegment> sides = new ArrayList<LineSegment>();

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
				sides.add(new LineSegment(lastPoint, nextPoint));
			} else if(segType == PathIterator.SEG_CLOSE) {
				//store a line from the last point to the last point we moved to
				sides.add(new LineSegment(lastPoint, moveToPoint));
			} else if(segType == PathIterator.SEG_CUBICTO || segType == PathIterator.SEG_QUADTO) {
				System.out.println("GOT A CURVE WHEN GETTING SIDES!!!! SHOULD NOT HAPPEN!!!");
			}

			lastPoint = (Point2D) nextPoint.clone();
			fpi.next();
		}

		return sides;
	}

	public static Point2D getNearestPoint(Shape onThisShape, Point2D toThisPoint) {
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
		double distToPointONClosestSide;
		if(Utilities.shouldEqualsZero(closestVector.getMagnitude())) {
			distToPointONClosestSide = 0.0;
		} else {
			distToPointONClosestSide = vectToThisPoint.scalerProjectionOnto(closestVector);
		}

		//rescale the closest side to that length, so now it's end point will be the closest point
		closestVector = closestVector.rescale(distToPointONClosestSide);

		//return that closest point we just found
		return closestVector.getP2();
	}

	public static Line2D getNearestSide(Shape onThisShape, Point2D toThisPoint) {
		//get all the sides of this shape
		List<LineSegment> sides = getSides(onThisShape);

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

	public static double getAngleFromZero(Point2D pivot, Point2D anglePoint) {
		Vector v1 = Vector.getHorizontalUnitVector(pivot);
		return getAngleBetween(v1.getP2(), anglePoint, pivot);
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

	public static boolean shouldEqualsZero(double d) {
		return equalsWithin(d, 0.0, SMALL_EPSILON);
	}
	
	public static boolean shouldBeEqual(double d1, double d2) {
		return equalsWithin(d1, d2, SMALL_EPSILON);
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
		return "(" + p.getX() + ", " + p.getY() + ")";
	}

	public static String lineToString(Line2D l) {
		if(l== null) return null;
		return pointToString(l.getP1()) + " --> " + pointToString(l.getP2());
	}

}
