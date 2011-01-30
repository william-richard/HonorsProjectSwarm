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
	public boolean causesRepulsion() {
		return true;
	}

	@Override
	public double repulsionMinDist() {
		return 1.0;
	}

	@Override
	public double repulsionMaxDist() {
		return Bot.DEFAULT_VISIBILITY_RADIUS;
	}

	@Override
	public double repulsionCurveShape() {
		return 4.5;
	}
	
	@Override
	public double repulsionScalingFactor() {
		return 100;
	}
	
}
