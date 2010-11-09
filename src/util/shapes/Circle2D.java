package util.shapes;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import util.Vector;

public class Circle2D extends Ellipse2D {

	Point2D.Double center;
	double radius;


	public Circle2D(double x, double y, double r) {
		this.setFrame(x, y, r, r);
	}

	public Circle2D(Point2D center, double r) {
		this(center.getX(), center.getY(), r);
	}

	@Override
	public double getHeight() {
		return radius*2;
	}

	@Override
	public double getWidth() {
		return radius*2;
	}

	@Override
	public double getX() {
		return center.getX() - radius;
	}

	@Override
	public double getY() {
		return center.getY() - radius;
	}

	@Override
	public boolean isEmpty() {
		return radius <= 0.0;
	}

	@Override
	public void setFrame(double x, double y, double w, double h) {
		if(w != h) {
			throw new IllegalArgumentException("Circle2D needs to have constant radius");
		}

		center = new Point2D.Double(x, y);
		radius = w;
	}

	@Override
	public Rectangle2D getBounds2D() {
		return new Rectangle2D.Double(this.getX(), this.getY(), getWidth(), getHeight());
	}

//	@Override
//	public boolean contains(double x, double y) {
//		double dxSquared = (x-this.getX())*(x-this.getX());
//		double dySquared = (y-this.getY())*(y-this.getY());
//		return radius*radius >= dxSquared + dySquared;
//	}
	
	/** For all of these methods, see the following website
	 * http://stackoverflow.com/questions/1073336/circle-line-collision-detection
	 */

	private double calculateLineCircleQuadraticAValue(Vector d) {
		return d.dot(d);
	}

	private double calculateLineCircleQuadraticBValue(Vector d, Vector f) {
		return 2*f.dot(d);
	}

	private double calculateLineCircleQuadraticCValue(Vector f) {
		return f.dot(f) - radius*radius;
	}
	

	public double lineCircleDiscriminant(Vector d, Vector f) {
		double a = calculateLineCircleQuadraticAValue(d);
		double b = calculateLineCircleQuadraticBValue(d,f);
		double c = calculateLineCircleQuadraticCValue(f);

		return b*b - 4*a*c;
	}

	public boolean intersectsLine(LineSegment line) {
		//make the line into a vector pointing towards the circle
		Vector d;
		if(line.getP1().distanceSq(center) > line.getP2().distanceSq(center)) {
			d = new Vector(line.getP1(), line.getP2());
		} else {
			d = new Vector(line.getP2(), line.getP1());
		}
		
		//also make the vector going from the center of the circle to the start of d
		Vector f = new Vector(center, d.getP1());

		return lineCircleDiscriminant(d,f) >= 0.0;
	}

	public List<Point2D> getLineIntersectionPoints(LineSegment line) {
		List<Point2D> results = new ArrayList<Point2D>();
		
		//before we start, make sure neither endpoint are inside the circle
		//we want those if they are there
		if(this.contains(line.getP1())) results.add(line.getP1());
		if(this.contains(line.getP2())) results.add(line.getP2());
		//there should be a max of 2 points - if we have both, stop
		if(results.size() == 2) {
			return results;
		}

		//make the line into a vector pointing towards the circle
		Vector d;
		if(line.getP1().distanceSq(center) > line.getP2().distanceSq(center)) {
			d = new Vector(line.getP1(), line.getP2());
		} else {
			d = new Vector(line.getP2(), line.getP1());
		}
		
		//also make the vector going from the center of the circle to the start of d
		Vector f = new Vector(center, d.getP1());

		double discriminant = lineCircleDiscriminant(d,f);
		if(discriminant < 0) {
			//no real roots
//			System.out.println("Negative discriminant");
			return results;
		}

		double a = calculateLineCircleQuadraticAValue(d);
		double b = calculateLineCircleQuadraticBValue(d,f);
		//from now on, we really want the square root of the discriminant rather than discriminant
		double sqrtDiscriminant = Math.sqrt(discriminant);
		
		double t1 = (-1*b + sqrtDiscriminant) /(2*a);
		double t2 = (-1*b - sqrtDiscriminant) /(2*a);
		
//		System.out.println("t1 = " + t1 + "\tt2 = " + t2);
		
		//t1 and t2 tell us how far along the d the points are
		//find them by rescaling the vector d if the points are on the line segment
		//i.e. 0 <= t# <= 1
		Vector toPoint;
		if(0.0<= t1 && t1 <= 1.0) {
			toPoint = d.rescaleRatio(t1);
			results.add(toPoint.getP2());
		}
		//if we have 2 points, stop so we don't double up
		if(results.size() == 2) {
			return results;
		}
		
		//get the last point, if it is there
		if(0.0 <= t2 && t2 <= 1.0) {
			toPoint = d.rescaleRatio(t2);
			results.add(toPoint.getP2());
		}
		
		return results;
	}
	
	public LineSegment getLineIntersectionSegment(LineSegment line) {
		//get the intersection points of the segment
		List<Point2D> intersectionPoints = getLineIntersectionPoints(line);
		
//		System.out.println("Got " + intersectionPoints.size() + " intersection points with circle");
		
		//There is a maximum of 2 points, but there could be 1 or 0
		//return a null if there are not 2 intersection points
		if(intersectionPoints.size() == 2) {
			return new LineSegment(intersectionPoints.get(0), intersectionPoints.get(1));
		}
		
		//couldn't make a segment - return a null
		return null;
	}



}
