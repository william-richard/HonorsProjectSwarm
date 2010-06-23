import java.awt.Color;
import java.awt.Polygon;


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
}
