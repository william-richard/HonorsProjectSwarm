import java.awt.Color;


public class SafeDebris extends SafeZone {

	private static final Color SafeDebrisColor = new Color(34, 139, 75);

	
	public SafeDebris(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID);
		setColor(SafeDebrisColor);
	}
	
	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY/2.0;
	}

}
