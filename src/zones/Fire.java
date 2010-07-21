package zones;

import java.awt.Color;

import simulation.Bot;


public class Fire extends DangerZone {

	private static final long serialVersionUID = 1L;
	private static final Color FireColor = new Color(255,140,0);
	
	public Fire(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, FireColor);
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY/5;
	}		
	
	@Override
	public boolean isObstacle() {
		return true;
	}

}
