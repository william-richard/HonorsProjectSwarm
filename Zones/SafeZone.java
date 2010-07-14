import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;


public class SafeZone extends Zone {

	private static final long serialVersionUID = 1L;
	private static final Color SafeZoneColor = new Color(34, 139, 34);

	public SafeZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID);
		setColor(SafeZoneColor);
	}

//	public SafeZone(Area a, int _zoneid) {
//		super(a, _zoneid, SafeZone.SafeZoneColor);
//	}

	
	@Override
	public Shape getAudibleRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_AUDITORY_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_AUDITORY_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFAULT_AUDITORY_RADIUS*2, Bot.DEFAULT_AUDITORY_RADIUS*2);
	}

	@Override
	public Shape getBroadcastRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFAULT_BROADCAST_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFAULT_BROADCAST_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFAULT_BROADCAST_RADIUS*2, Bot.DEFAULT_BROADCAST_RADIUS*2);
	}

	@Override
	public Shout getShout(Victim shouter) {
		//for now, the shout is a circle of the default radius

		//calculate it's corner
		double cornerX = shouter.getCenterX() - Shout.DEFAULT_SHOUT_RADIUS;
		double cornerY = shouter.getCenterY()- Shout.DEFAULT_SHOUT_RADIUS;

		//return the circular shout
		return new Shout(new Ellipse2D.Double(cornerX, cornerY, Shout.DEFAULT_SHOUT_RADIUS*2, Shout.DEFAULT_SHOUT_RADIUS*2), shouter);	
	}

	@Override
	public Shape getVisibilityRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double broadcastRangeCornerX = originator.getX() - Bot.DEFALUT_VISIBILITY_RADIUS;
		double broadcastRangeCornerY = originator.getY() - Bot.DEFALUT_VISIBILITY_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, Bot.DEFALUT_VISIBILITY_RADIUS*2, Bot.DEFALUT_VISIBILITY_RADIUS*2);
	}


	@Override
	public Shape getFoundRange(Point2D originator) {
		//in this case, return a circle
		//know the center of the circle, and the radius - need to find the corner
		double foundRangeCornerX = originator.getX() - Bot.DEFAULT_FOUND_RANGE;
		double foundRangeCornerY = originator.getY() - Bot.DEFAULT_FOUND_RANGE;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(foundRangeCornerX, foundRangeCornerY, Bot.DEFAULT_FOUND_RANGE*2, Bot.DEFAULT_FOUND_RANGE*2);
	}

	@Override
	public double getBotMaxVelocity() {
		return Bot.DEFAULT_MAX_VELOCITY;
	}
}
