package util.shapes;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import util.Utilities;

public class LineSegment extends Line2D implements Serializable {

	private static final long serialVersionUID = 1032095923764472352L;

	private Point2D p1;
	private Point2D p2;

	private static final boolean INTERSECTION_DEBUG = false;

	public LineSegment(Point2D _p1, Point2D _p2) {
		p1 = _p1;
		p2 = _p2;
	}

	public LineSegment(Line2D _line) {
		this(_line.getP1(), _line.getP2());
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

	@Override
	public double getY1() {
		return p1.getY();
	}

	@Override
	public double getY2() {
		return p2.getY();
	}
	
	public double getLength() {
		return p1.distance(p2);
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

		if(Utilities.shouldEqualsZero(rectHeight)) {
			rectHeight = 1.0;
		}

		if(Utilities.shouldEqualsZero(rectWidth)) {
			rectWidth = 1.0;
		}


		return new Rectangle2D.Double(rectX, rectY, rectWidth, rectHeight);
	}

	@Override
	public String toString() {
		return Utilities.lineToString(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof LineSegment))
			return false;
		LineSegment other = (LineSegment) obj;
		//now test if both points are equal
		if( (this.getP1().equals(other.getP1()) && (this.getP2().equals(other.getP2()))) 
				|| (this.getP1().equals(other.getP2())) && (this.getP2().equals(other.getP1())))
			return true;
		return false;

	}

	public double getSlope() {
		return (getY2() - getY1())/(getX2() - getX1());
	}

	public double getYInercept() {
		return getY1() - (getSlope() * getX1());
	}

	public boolean isVerctical() {
		return Utilities.equalsWithin(getX1(), getX2(), Utilities.SMALL_EPSILON);
	}

	public boolean isHorizontal() {
		return Utilities.equalsWithin(getY1(), getY2(), Utilities.SMALL_EPSILON);
	}

	public boolean pointInLineSegment(Point2D p) {
		if(INTERSECTION_DEBUG) {
			System.out.println("Computing if " + p + " is on this line segment: " + this);
			if(p.getX() == java.lang.Double.NaN) {
				System.out.println("Someone is vertical");
			}
			System.out.println("ptLineDist = " + ptLineDist(p));
			System.out.println(this.getBounds2D());
			System.out.println("Bounds contains p: " + this.getBounds2D().contains(p));
		}
		return Utilities.shouldEqualsZero(ptLineDist(p)) && this.getBounds2D().contains(p);
	}

	public boolean pointInLineSegment(double x, double y) {
		return pointInLineSegment(new Point2D.Double(x,y));
	}

	/**
	 * Get the intersection point of these lines if they went on forever
	 * @param other
	 * @return
	 */
	public Point2D intersectionPoint(LineSegment other) {
		if(INTERSECTION_DEBUG) {
			System.out.println("Computing intersection point on lines " + this + " and " + other);
		}

		/* Check for we are vertical first
		 */
		if(this.isVerctical()) {
			if(other.isVerctical()) {
				//both vertical
				//either all points intersect, or no points
				//either way, this isn't useful
				return null;
			} else if(other.isHorizontal()) {
				//take the x value from this line, and the y value from the other line
				return new Point2D.Double(this.getX1(), other.getY1());
			} else {
				//just we are vertical
				//find the point on the other line that matches our x value
				double intersectionY = other.getYValue(this.getX1());
				return new Point2D.Double(this.getX1(), intersectionY);
			}
		}


		/* Then check if we are horizontal
		 */
		if(this.isHorizontal()) {
			if(other.isVerctical()) {
				//one horizontal and one vertical
				//find the intersection point
				return new Point2D.Double(other.getX1(), this.getY1());
			} else if(other.isHorizontal()) {
				//both horizontal
				//not useful
				return null;
			} else {
				//just we are horizontal
				//find the corresponding x value for the intersection point
				//if y = mx + b
				// then x = (y-b)/m
				double intersectionX = other.getXValue(this.getY1());
				return new Point2D.Double(intersectionX, this.getY1());
			}
		}

		//we have checked if we are horizontal or vertical
		//we aren't
		//but the other may be
		if(other.isVerctical()) {
			//we are not horizontal or vertical, but other is vertical
			//use other's x value to find the intersection point
			double intersectionY = this.getYValue(other.getX1());
			return new Point2D.Double(other.getX1(), intersectionY);
		}
		if(other.isHorizontal()) {
			//other is horizontal
			//use it's y value for the intersection point
			double intersctionX = this.getXValue(this.getY1());
			return new Point2D.Double(intersctionX, other.getY1());
		}

		/*if m1 is slope of this line, m2 is the slope of the other line
		 * b1 is the y intersept of this line and b2 is the y intersept of the other line
		 * the intersection's point should have the coordinates
		 * x = (b2-b1) / (m1-m2)
		 * y = m1 * x + b1
		 */
		double m1 = this.getSlope();
		double m2 = other.getSlope();
		double b1 = this.getYInercept();
		double b2 = other.getYInercept();

		double intersectionX = (b2-b1)/(m1-m2);
		double intersectionY = m1 * intersectionX + b1;

		Point2D.Double intersectionPoint = new Point2D.Double(intersectionX, intersectionY);
		
		return intersectionPoint;
		
	}
	
	public Point2D intersectionPoint(Line2D line) {
		return intersectionPoint(new LineSegment(line));
	}
	
	public boolean segmentsIntersect(LineSegment other) {
		Point2D intersectionPoint = intersectionPoint(other);
		if(intersectionPoint == null) {
			//lines are both either horizontal or vertical
			//and the lines are either the same or not
			if(this.isVerctical()) {
				//check for same x values
				return Utilities.equalsWithin(this.getX1(), other.getX1(), Utilities.SMALL_EPSILON);
			} else {
				//horizontal
				//check for same y values
				return Utilities.equalsWithin(this.getY1(), other.getY1(), Utilities.SMALL_EPSILON);
			}
		}

		return this.pointInLineSegment(intersectionPoint) && other.pointInLineSegment(intersectionPoint);
	}

	public boolean segmentsIntersect(Line2D other) {
		return segmentsIntersect(new LineSegment(other));
	}

	public double getLeastAngleToEndpoint(Point2D viewpoint) {
		double dx1 = p1.getX() - viewpoint.getX();
		double dy1 = p1.getY() - viewpoint.getY();
		double dx2 = p2.getX() - viewpoint.getX();
		double dy2 = p2.getY() - viewpoint.getY();

		double a1 = Math.atan(dy1/dx1);
		double a2 = Math.atan(dy2/dx2);

		return Math.min(a1, a2);
	}

	public double getGreatestAngleToEndpoint(Point2D viewpoint) {
		double dx1 = p1.getX() - viewpoint.getX();
		double dy1 = p1.getY() - viewpoint.getY();
		double dx2 = p2.getX() - viewpoint.getX();
		double dy2 = p2.getY() - viewpoint.getY();

		double a1 = Math.atan(dy1/dx1);
		double a2 = Math.atan(dy2/dx2);

		return Math.max(a1, a2);
	}

	public Point2D getMidpoint() {
		double midX = (p1.getX() + p2.getX()) / 2.0;
		double midY = (p1.getY() + p2.getY()) / 2.0;
		
		return new Point2D.Double(midX, midY);
	}
	
	private double getYValue(double x) {
		return getSlope() * x + getYInercept();
	}
	
	private double getXValue(double y) {
		return (y - getYInercept()) / getSlope();
	}
	
	public Set<Point> getIntegerPoints() { 
		HashSet<Point> result = new HashSet<Point>();
		//start with endpoint with min integer x value and go to the other endpoint
		//going to have to mess with that endpoint a bit
		int minIntEndX = getX1() < getX2() ? (int) getX1() : (int) getX2();
		int maxIntEndX = getX1() >= getX2() ? (int) getX1() : (int) getX2();
		
		if(minIntEndX == maxIntEndX) {
			//vertical line
			//going to have to do integer y values instead
			int minIntEndY = getY1() < getY2() ? (int) getY1() : (int) getY2();
			int maxIntEndY = getY1() >= getY2() ? (int) getY1() : (int) getY2();
			
			for(int i = minIntEndY; i <= maxIntEndY; i++) {
				result.add(new Point((int)getXValue(i), i));
			}
		} else {
			for(int i = minIntEndX; i <= maxIntEndX; i++) {
				result.add(new Point(i, (int)getYValue(i)));
			}
		}
		
		return result;
	}
	
	
}
