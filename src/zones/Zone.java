package zones;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;

public abstract class Zone extends Polygon {

	private static final long serialVersionUID = -3828823916299213104L;

	protected int zoneID;
	protected Color zoneColor;
	
	protected Zone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		super(xPoints, yPoints, numPoints);
		zoneID = _zoneID;
		zoneColor = _zoneColor;
	}
	
	protected Zone(int _zoneID, Color _zoneColor) {
		super();
		zoneID = _zoneID;
	}
		
	public Color getColor() {
		return zoneColor;
	}
	
	public int getID() {
		return zoneID;
	}
	
	public double getCenterX() {
		return this.getBounds2D().getCenterX();
	}
	
	public double getCenterY() {
		return this.getBounds2D().getCenterY();
	}
	
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}
	
	protected void setColor(Color newColor) {
		zoneColor = newColor;
	}
	
	public Circle2D getBroadcastArea(Point2D originator) {
		return new Circle2D(originator, getBroadcastRange(originator));
	}
	
	public Circle2D getVisibilityArea(Point2D originator) {
		return new Circle2D(originator, getVisiblityRange(originator));
	}
	
	public Circle2D getAudibleArea(Point2D originator) {
		return new Circle2D(originator, getAudibleRange(originator));
	}
	
	public Circle2D getFoundArea(Point2D originator) {
		return new Circle2D(originator, getFoundRange(originator));
	}

	
	public abstract Shout getShout(Survivor shouter);
	public abstract double getBroadcastRange(Point2D originator);
	public abstract double getVisiblityRange(Point2D originator);
	public abstract double getAudibleRange(Point2D originator);
	public abstract double getFoundRange(Point2D originator);
	
	public abstract boolean isObstacle();
}
