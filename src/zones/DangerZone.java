package zones;

import java.awt.Color;
import java.awt.geom.Point2D;

import simulation.Bot;
import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;


public class DangerZone extends Zone {
	
	private static final long serialVersionUID = 1L;

	private static final Color DangerZoneColor = new Color(139,37,0);
	
	public DangerZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		this(xPoints, yPoints, numPoints, _zoneID, DangerZoneColor);
	}
	
	protected DangerZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		super(xPoints, yPoints, numPoints, _zoneID, _zoneColor);
	}
	
	@Override
	public Shout getShout(Survivor shouter) {
		//for now, the shout is a circle of the default radius
		//return the circular shout
		return new Shout(new Circle2D(shouter.getCenterLocation(), Shout.DEFAULT_SHOUT_RADIUS), shouter);
	}

	@Override
	public boolean isObstacle() {
		return false;
	}

	@Override
	public double getAudibleRange(Point2D originator) {
		return Bot.DEFAULT_AUDITORY_RADIUS;
	}

	@Override
	public double getBroadcastRange(Point2D originator) {
		return Bot.DEFAULT_BROADCAST_RADIUS;
	}

	@Override
	public double getFoundRange(Point2D originator) {
		return Bot.DEFAULT_FOUND_RANGE;
	}

	@Override
	public double getVisiblityRange(Point2D originator) {
		return Bot.DEFALUT_VISIBILITY_RADIUS;
	}
	
	public double getRepulsionForcePerLength() {
		//length measured in pixels, not meters.
		//think of this as the 
		return 6.0;
	}
}
