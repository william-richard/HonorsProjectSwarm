import java.awt.Color;
import java.awt.Polygon;


public abstract class Zone extends Polygon {

	private static final long serialVersionUID = 1L;
	
	private Color zoneColor;
	
	
	public Zone(int[] xPoints, int[] yPoints, int numPoints) {
		super(xPoints, yPoints, numPoints);
	}
	
	public Color getZoneColor() {
		return zoneColor;
	}

}
