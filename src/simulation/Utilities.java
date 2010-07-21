package simulation;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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
		Area a1 = new Area(s1);
		Area a2 = new Area(s2);
		
		if(a1.isEmpty()) {
			a1 = new Area(giveArealessShapeArea(s1));
		}
		
		if(a2.isEmpty()) {
			a2 = new Area(giveArealessShapeArea(s2));
		}

		Area intersection = new Area(s1);
		intersection.intersect(a2);

		if(intersection.isEmpty()) return false;
		if(intersection.equals(a1) || intersection.equals(a2)) return false;

		return true;
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
		//get the PathIterator of s
		PathIterator pi = s.getPathIterator(null);

		//now, go through the iterator and extract the verticies

		double[] curCoords = new double[6];
		List<Point2D> verticies = new ArrayList<Point2D>();

		while(! pi.isDone()) {
			int segType = pi.currentSegment(curCoords);
			if(segType == PathIterator.SEG_QUADTO || segType == PathIterator.SEG_CUBICTO) {
//				System.out.println("GET VECTICES HAS RUN INTO A SEGMENT IT CAN'T DEAL WITH!!!!)");
//				System.out.println(s);
//				System.exit(0);
				pi.next();
				continue;
			}

			Point2D newPoint = new Point2D.Double(curCoords[0], curCoords[1]);

			//don't want to add multiples
			if(! (verticies.indexOf(newPoint) >= 0)) {

//				if(segType == PathIterator.SEG_CLOSE) {
//					//we're done with the shape, so break out of the while
//					break;
//				}
				
				//add the point
				verticies.add(newPoint);
			}
			
			pi.next();
		}

		return verticies;

	}
	
	public static List<Line2D> getSides(Shape s) {
		//get s's PathIterator
		PathIterator pi = s.getPathIterator(null);
		
		//now, go through the iterator and extract the sides
		double[] curCoords = new double[6];
		List<Line2D> sides = new ArrayList<Line2D>();
		
		Point2D moveToPoint = new Point2D.Double();
		Point2D lastPoint = new Point2D.Double();
		
		while(! pi.isDone()) {
			int segType = pi.currentSegment(curCoords);

			Point2D nextPoint = new Point2D.Double(curCoords[0], curCoords[1]);
			
			if(segType == PathIterator.SEG_QUADTO || segType == PathIterator.SEG_CUBICTO) {
				System.out.println("GET SIDES HAS RUN INTO A SEGMENT IT CAN'T DEAL WITH!!!!)");
				System.out.println(s);
				System.exit(0);
			} else if(segType == PathIterator.SEG_MOVETO) {
				//store the moveto point
				moveToPoint = nextPoint;
			} else if(segType == PathIterator.SEG_LINETO) {
				//store a line from the last point to the point we just got
				sides.add(new Line2D.Double(lastPoint, nextPoint));
			} else if(segType == PathIterator.SEG_CLOSE) {
				//store a line from the last point to the last point we moved to
				sides.add(new Line2D.Double(lastPoint, moveToPoint));
			}
			
			lastPoint = nextPoint;
			pi.next();
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
		double maxAngleBetween = Double.MIN_VALUE;
		
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
	
}
