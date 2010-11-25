package zones;

import java.awt.Color;
import java.awt.geom.Point2D;

import simulation.Bot;
import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;


public class SafeZone extends Zone {

	private static final long serialVersionUID = 1L;
	private static final Color SafeZoneColor = new Color(34, 139, 34);

	public SafeZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		this(xPoints, yPoints, numPoints, _zoneID, SafeZoneColor);
	}

	protected SafeZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID, Color _zoneColor) {
		super(xPoints, yPoints, numPoints, _zoneID, _zoneColor);
	}
	
	
	@Override
	public Circle2D getAudibleArea(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_AUDITORY_RADIUS);
	}

	@Override
	public Circle2D getBroadcastArea(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_BROADCAST_RADIUS);
	}

	@Override
	public Circle2D getVisibilityArea(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFALUT_VISIBILITY_RADIUS);
	}


	@Override
	public Circle2D getFoundArea(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_FOUND_RANGE);
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

	@Override
	public double getRepulsionForcePerLength() {
		return 0.0;
	}
}
