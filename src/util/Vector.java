package util;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class Vector extends Line2D{

	private Point2D p1;
	private Point2D p2;

	public Vector() {
		super();
		p1 = new Point2D.Double();
		p2 = new Point2D.Double();
	}

	public Vector(Point2D _p1, Point2D _p2) {
		this();
		p1 = _p1;
		p2 = _p2;
	}

	public Vector(double x1, double y1, double x2, double y2) {
		this(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
	}

	public Vector(Point2D _p1, Point2D _p2, double mag) {
		this(_p1, _p2);
		this.setLine(this.rescale(mag));
	}

	public Vector(double x1, double y1, double x2, double y2, double mag) {
		this(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), mag);
	}

	public Vector(Line2D l) {
		this(l.getP1(), l.getP2());
	}

	@Override
	public Point2D getP1() {	
		return p1;	
	}

	@Override
	public Point2D getP2() {
		return p2;
	}

	@Override
	public double getX1() {
		return p1.getX();
	}

	@Override
	public double getX2() {
		return p2.getX();
	}

	public double getXMag() {
		return getX2() - getX1();
	}

	public Vector getXComponent() {
		return new Vector(this.getX1(), this.getY1(), this.getX1() + 1.0, this.getY1(), this.getXMag());
	}

	@Override
	public double getY1() {
		return p1.getY();
	}

	@Override
	public double getY2() {
		return p2.getY();
	}

	public double getYMag() {
		return getY2() - getY1();
	}

	public Vector getYComponent() {
		return new Vector(this.getX1(), this.getY1(), this.getX1(), this.getY1() + 1.0, this.getYMag());
	}

	public Vector getUnitVector() {
		return this.rescale(1.0);
	}


	public double getMagnitude() {
		return p1.distance(p2);
	}

	public double getMagSquare() {
		return p1.distanceSq(p2);
	}

	@Override
	public void setLine(double x1, double y1, double x2, double y2) {
		p1 = new Point2D.Double(x1, y1);
		p2 = new Point2D.Double(x2, y2);
	}

	@Override
	public Rectangle2D getBounds2D() {
		double rectX = p1.getX() < p2.getX() ? p1.getX() : p2.getX();
		double rectY = p1.getY() < p2.getY() ? p1.getY() : p2.getY();
		double rectWidth = Math.abs(p1.getX() - p2.getX());
		double rectHeight = Math.abs(p1.getY() - p2.getY());

		return new Rectangle2D.Double(rectX, rectY, rectWidth, rectHeight);
	}

	public Vector translate(double deltaX, double deltaY) {
		Point2D newP1 = new Point2D.Double(p1.getX() + deltaX, p1.getY() + deltaY);
		Point2D newP2 = new Point2D.Double(p2.getX() + deltaX, p2.getY() + deltaY);
		return new Vector(newP1, newP2);
	}

	public Vector moveTo(Point2D newP1) {
		double deltaX = newP1.getX() - p1.getX();
		double deltaY = newP1.getY() - p1.getY();
		return translate(deltaX, deltaY);
	}

	public Vector add(Vector addVector) {
		//basically, we can do this graphically
		//move the addVector so that it starts at our end
		//then make a vector from our start to the end of the other vector
		Vector movedVect = addVector.moveTo(this.getP2());
		return new Vector(this.getP1(), movedVect.getP2());
	}

	public double dot(Vector other) {
		return (this.getXMag()*other.getXMag()) + (this.getYMag()*other.getYMag());
	}

	public Vector rescaleRatio(double ratio) {		
		if(java.lang.Double.isInfinite(ratio)) {
			//			System.out.println("Trying to rescale by infinity - ignoring");
			return this;
		}

		double newX2 = this.getX1() + (ratio*getXMag());
		double newY2 = this.getY1() + (ratio*getYMag());
		return new Vector(this.getP1(), new Point2D.Double(newX2, newY2));
	}

	public Vector rescale(double newMag) {
		return rescaleRatio(newMag/this.getMagnitude()); 
	}

	public Vector reverse() {
		return this.rotate(Math.PI);
	}

	public Vector rotate(double radians) {
		/* want to rotate P2 about P1 clockwise
		 * so, we want to use newX = X*cos(angle) - Y*sin(angle)
		 * and newY = X*sin(angle) + Y*cos(angle)
		 * but we need first need to translate so that P1 is the origin, then do the calculation
		 * and then translate back
		 */
		Point2D translatedP2 = new Point2D.Double(this.getX2() - this.getX1(), this.getY2() - this.getY1());
		double rotatedTranslatedX = translatedP2.getX()*Math.cos(radians) - translatedP2.getY()*Math.sin(radians);
		double rotatedTranslatedY = translatedP2.getX()*Math.sin(radians) + translatedP2.getY()*Math.cos(radians);
		Point2D rotatedP2 = new Point2D.Double(rotatedTranslatedX + this.getX1(), rotatedTranslatedY + this.getY1());
		return new Vector(this.getP1(), rotatedP2);
	}
	
	public Vector rotateDegrees(double degrees) {
		return this.rotate(degrees * Math.PI / 180.0);
	}

	/*get the scalar projection of this on other
	 * i.e. get the magnitude of the component of the this vector in the direction of the other vector
	 */
	public double scalerProjectionOnto(Vector other) {
		return this.dot(other.getUnitVector());
	}

	public Vector getParallelVector(Point2D startPoint, double magnitude) {
		Vector parallelVector = this.moveTo(startPoint);
		return parallelVector.rescale(magnitude);
	}
	
	public Vector getPerpendicularVector(Point2D startPoint, double magnitude) {
		Vector perpVect = this.moveTo(startPoint);
		perpVect = perpVect.rotate(Math.PI/2.0);
		return perpVect.rescale(magnitude);
	}
	
	public Vector getPerpendicularVectorPointedTowards(Point2D startPoint, double magnitude, Point2D directionPoint) {
		Vector perpVect = this.moveTo(startPoint);
		if(perpVect.relativeCCW(directionPoint) > 0) {
			//we need to turn counter clockwise
			perpVect = perpVect.rotate(-1 * Math.PI / 2.0);
		} else {
			//we'll rotate clockwise
			perpVect = perpVect.rotate(Math.PI / 2.0);
		}
		return perpVect.rescale(magnitude);
	}

	public Point2D getClosestIntersectionToStart(Shape withThisShape) {
		//first, get the sides of the shape
		List<Line2D> shapeSides = Utilities.getSides(withThisShape);

		//go through all the sides, and see if there are any intersections
		//there may be more than 1
		List<Point2D> shapeIntersectionPoints = new ArrayList<Point2D>();

		for(Line2D curSide : shapeSides) {
			if(curSide.intersectsLine(this)) {
				Point2D intersectionPoint = Utilities.getIntersectionPoint(this, curSide);
				shapeIntersectionPoints.add(intersectionPoint);
			}
		}

		if(shapeIntersectionPoints.size() <= 0) {
			return null;
		} else if(shapeIntersectionPoints.size() == 1) {
			return shapeIntersectionPoints.get(0);
		} else {
			//get the point closest to P1
			Point2D closestIntersectionPoint = null;
			double closestIntersectionPointDistSq = java.lang.Double.MAX_VALUE;
			for(Point2D curPoint : shapeIntersectionPoints) {
				double curDistSq = curPoint.distanceSq(this.getP1());
				if(curDistSq < closestIntersectionPointDistSq) {
					closestIntersectionPointDistSq = curDistSq;
					closestIntersectionPoint = curPoint;
				}
			}
			return closestIntersectionPoint;
		}
	}

	/*get the angle between this and other
	 * 
	 */
	public double getAngleBetween(Vector other) {
		return Math.acos(this.dot(other) / (this.getMagnitude() * other.getMagnitude()));
	}
	
	public Point2D getMidpoint() {
		//get the mid x value
		double xMid = (getX1() + getX2()) / 2.0;
		//get the mid y value
		double yMid = (getY1() + getY2()) /2.0;
		//return the point
		return new Point2D.Double(xMid, yMid);
		
	}

	public String toString() {
		return "(" + this.getX1() + ", " + this.getY1() + ") (" + this.getX2() + ", " + this.getY2() + ")  " + this.getMagnitude();
	}
}
