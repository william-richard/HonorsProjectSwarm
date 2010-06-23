import java.awt.Color;


public class SafeZone extends Zone {

	private static final long serialVersionUID = 1L;

	private final Color zoneColor = new Color(34,139,34);
	
	public SafeZone(int[] points, int[] points2, int numPoints) {
		super(points, points2, numPoints);
	}
	
	public Color getZoneColor() {
		return zoneColor;
	}
}
