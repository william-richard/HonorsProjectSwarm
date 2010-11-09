package util.shapes;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;

import util.Utilities;

public class LineSegment extends Line2D {

	private Point2D p1;
	private Point2D p2;
	
	public LineSegment(Point2D _p1, Point2D _p2) {
		p1 = _p1;
		p2 = _p2;
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
	
	public double getSlope() {
		return (getY2() - getY1())/(getX2() - getX1());
	}
	
	public double getYInercept() {
		return getY1() - (getSlope() * getX1());
	}
	
	public boolean pointOnLine(Point2D p) {
		return ptLineDist(p) == 0.0 && getBounds2D().contains(p);
	}
	
	public boolean pointInLineSegment(double x, double y) {
		return pointOnLine(new Point2D.Double(x,y));
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
	
}
