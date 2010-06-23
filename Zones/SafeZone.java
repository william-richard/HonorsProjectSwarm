import java.awt.Color;


public class SafeZone extends Zone {

	private static final long serialVersionUID = 1L;
	
	public SafeZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, new Color(34,139,34));
	}
}
