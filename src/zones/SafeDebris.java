package zones;

import java.awt.Color;

import simulation.Bot;


public class SafeDebris extends SafeZone {

	private static final long serialVersionUID = 1L;
	private static final Color SafeDebrisColor = new Color(34, 139, 75);

	
	public SafeDebris(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, SafeDebrisColor);
	}
	
}
