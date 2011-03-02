package zones;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Bot;
import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;


public class BaseZone extends Zone {

	private static final long serialVersionUID = 1L;
	private final static Color BaseZoneColor = new Color(0, 100, 0);

	public BaseZone(List<GraphEdge> _sides, int _zoneID, Point2D center, BoundingBox bbox) {
		super(_sides, _zoneID, center, bbox);
	}
	
	public BaseZone(Zone other) {
		super(other);
	}
	
	

	@Override
	public boolean isObstacle() {
		return false;
	}

	@Override
	public double getAudibleRange() {
		return Bot.DEFAULT_AUDITORY_RADIUS;
	}

	@Override
	public double getBroadcastRange() {
		return Bot.DEFAULT_BROADCAST_RADIUS;
	}

	@Override
	public double getFoundRange() {
		return Bot.DEFAULT_FOUND_RANGE;
	}

	@Override
	public double getVisiblityRange() {
		return Bot.DEFAULT_VISIBILITY_RADIUS;
	}

	@Override
	public Shout getShout(Survivor shouter) {
		return new Shout(new Circle2D(shouter.getCenterLocation(), Shout.DEFAULT_SHOUT_RADIUS), shouter);
	}

	@Override
	public boolean causesRepulsion() {
		return false;
	}

	@Override
	public double repulsionMinDist() {
		return 0;
	}

	@Override
	public double repulsionMaxDist() {
		return 0;
	}

	@Override
	public double repulsionCurveShape() {
		return 0;
	}

	@Override
	public double repulsionScalingFactor() {
		return 0;
	}

	@Override
	public Color getColor() {
		return BaseZoneColor;
	}
	
}
