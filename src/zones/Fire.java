package zones;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Bot;

public class Fire extends DangerZone {

	//TODO: Make file destroy bots
	
	private static final long serialVersionUID = 1L;
	private static final Color FireColor = new Color(255,140,0);
	
	public Fire(List<GraphEdge> _sides, int _zoneID, Point2D center, BoundingBox bbox) {
		super(_sides, _zoneID, center, bbox);
	}
	
	public Fire(Zone other) {
		super(other);
	}
	
	
	@Override
	public Color getColor() {
		return FireColor;
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
		return Bot.DEFAULT_VISIBILITY_RADIUS * 1.1;
	}

	@Override
	public double repulsionCurveShape() {
		return 4.5;
	}
	
	@Override
	public double repulsionScalingFactor() {
		return 80;
	}
	
	@Override
	public double getPathWeightPerPixel() {
		return 5;
	}

	
	
}
