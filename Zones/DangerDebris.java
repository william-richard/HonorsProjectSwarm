import java.awt.Color;


public class DangerDebris extends DangerZone {

	private static final Color DangerDebrisColor = new Color(139,37,65);
	
	public DangerDebris(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID);
		setColor(DangerDebrisColor);
	}
	
	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY/2.0;
	}	

}
