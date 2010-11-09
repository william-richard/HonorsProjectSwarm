package zones;

import java.awt.Color;

import simulation.Bot;


public class DangerDebris extends DangerZone {

	private static final long serialVersionUID = 1L;
	private static final Color DangerDebrisColor = new Color(139,37,65);
	
	public DangerDebris(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, DangerDebrisColor);
	}
	
}
