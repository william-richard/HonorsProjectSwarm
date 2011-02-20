package zones;

import java.awt.Color;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Bot;

public class Fire extends DangerZone {

	//TODO: Make file destroy bots
	
	private static final long serialVersionUID = 1L;
	private static final Color FireColor = new Color(255,140,0);
	
	public Fire(List<GraphEdge> _sides, int _zoneID) {
		super(_sides, _zoneID);
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
		return Bot.DEFAULT_VISIBILITY_RADIUS;
	}

	@Override
	public double repulsionCurveShape() {
		return 4.5;
	}
	
	@Override
	public double repulsionScalingFactor() {
		return 115;
	}
	
}
