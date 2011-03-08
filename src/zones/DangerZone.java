package zones;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Bot;
import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;


public class DangerZone extends Zone {
	
	private static final long serialVersionUID = 1L;

	private static final Color DangerZoneColor = new Color(139,37,0);
		
	public DangerZone(List<GraphEdge> _sides, int _zoneID, Point2D center, BoundingBox bbox) {
		super(_sides, _zoneID, center, bbox);
	}
	
	public DangerZone(Zone other) {
		super(other);
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
	public boolean causesRepulsion() {
		return true;
	}

	@Override
	public double repulsionMinDist() {
		return 0.1;
	}

	@Override
	public double repulsionMaxDist() {
		return Bot.DEFAULT_VISIBILITY_RADIUS * 1.5;
	}

	@Override
	public double repulsionCurveShape() {
		return 5;
	}

	@Override
	public double repulsionScalingFactor() {
		return 60;
	}

	@Override
	public Color getColor() {
		return DangerZoneColor;
	}
	
	
	
	
}
