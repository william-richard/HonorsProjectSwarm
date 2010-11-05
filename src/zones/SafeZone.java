package zones;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
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
	public Circle2D getAudibleRange(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_AUDITORY_RADIUS);
	}

	@Override
	public Circle2D getBroadcastRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_BROADCAST_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_BROADCAST_RADIUS;

		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_BROADCAST_RADIUS);
	}

	@Override
	public Shout getShout(Survivor shouter) {
		//for now, the shout is a circle of the default radius

		//calculate it's corner
		double cornerX = shouter.getCenterX() - Shout.DEFAULT_SHOUT_RADIUS;
		double cornerY = shouter.getCenterY()- Shout.DEFAULT_SHOUT_RADIUS;

		//return the circular shout
		return new Shout(new Ellipse2D.Double(cornerX, cornerY, Shout.DEFAULT_SHOUT_RADIUS*2, Shout.DEFAULT_SHOUT_RADIUS*2), shouter);	
	}

	@Override
	public Circle2D getVisibilityRange(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFALUT_VISIBILITY_RADIUS);
	}


	@Override
	public Circle2D getFoundRange(Point2D originator) {
		//now, make the broadcast range shape
		return new Circle2D(originator, Bot.DEFAULT_FOUND_RANGE);
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY;
	}
	
	@Override
	public boolean isObstacle() {
		return false;
	}
}
