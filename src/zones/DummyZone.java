package zones;

import java.awt.Color;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Shout;
import simulation.Survivor;

public class DummyZone extends Zone {

	public DummyZone(List<GraphEdge> _sides, int _zoneID) {
		super(_sides, _zoneID);
	}
	
	public DummyZone(Zone other) {
		super(other);
	}
	
	
	@Override
	public Shout getShout(Survivor shouter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getBroadcastRange() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getVisiblityRange() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAudibleRange() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFoundRange() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean causesRepulsion() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double repulsionMinDist() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double repulsionMaxDist() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double repulsionCurveShape() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double repulsionScalingFactor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isObstacle() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Color getColor() {
		return Color.white;
	}

}
