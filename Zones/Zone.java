import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Point2D;


public abstract class Zone extends Polygon {

	private static final long serialVersionUID = 1L;
	
	protected int zoneID;
	protected Color zoneColor;
	
	private Zone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints);
		zoneID = _zoneID;
	}
	
	protected Zone(int [] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		this(xPoints, yPoints, numPoints, _zoneID);
		zoneColor = _zoneColor;
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
	
	
	public abstract Shape getBroadcastRange(Point2D originator);
	public abstract Shape getVisibilityRange(Point2D originator);
	public abstract Shape getAudibleRange(Point2D originator);
	public abstract Shout getShout(Point2D originator);
	
	
}
