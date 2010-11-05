package util.shapes;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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

}
