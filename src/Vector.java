import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
		this.rescale(mag);
	}
	
	public Vector(double x1, double y1, double x2, double y2, double mag) {
		this(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), mag);
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
		return getX1() - getX2();
	}
	
	public Vector getXVect() {
		return new Vector(this.getP1().getX(), this.getP1().getY(), this.getP1().getX() + 1.0, this.getP1().getY(), this.getXMag());
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
		return getY1() - getY2();
	}
	
	public Vector getYVect() {
		return new Vector(this.getP1().getX(), this.getP1().getY(), this.getP1().getX(), this.getP1().getY() + 1.0, this.getYMag());
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
		
		double newX2 = this.getX1() - (ratio*getXMag());
		double newY2 = this.getY1() - (ratio*getYMag());
		return new Vector(this.getP1(), new Point2D.Double(newX2, newY2));
	}
	
	public Vector rescale(double newMag) {
		return rescaleRatio(newMag/this.getMagnitude()); 
	}
	
	public Vector reverse() {
		return this.rescaleRatio(-1.0);
	}
	
	public String toString() {
		return "(" + this.getX1() + ", " + this.getY1() + ") (" + this.getX2() + ", " + this.getY2() + ")  " + this.getMagnitude();
	}
}
