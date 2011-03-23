package zones;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Shout;
import simulation.Survivor;

public class DummyZone extends Zone {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1603350907162013018L;

	public DummyZone(List<GraphEdge> _sides, int _zoneID, Point2D center, BoundingBox bbox) {
		super(_sides, _zoneID, center, bbox);
	}
	
	public DummyZone(Zone other) {
		super(other);
	}
	
	
	@Override
	public Shout getShout(Survivor shouter) {
		return null;
	}

	@Override
	public double getBroadcastRange() {
		return 0;
	}

	@Override
	public double getVisiblityRange() {
		return 0;
	}

	@Override
	public double getAudibleRange() {
		return 0;
	}

	@Override
	public double getFoundRange() {
		return 0;
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
	public boolean isObstacle() {
		return false;
	}

	@Override
	public Color getColor() {
		return Color.white;
	}

	@Override
	public double getPathWeightPerPixel() {
		return 1;
	}

}
