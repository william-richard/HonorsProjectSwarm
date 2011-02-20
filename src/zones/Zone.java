package zones;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Shout;
import simulation.Survivor;
import util.shapes.Circle2D;

public abstract class Zone extends Polygon {

	private static final long serialVersionUID = -3828823916299213104L;

	protected int zoneID;
	protected int[] neighbors;
	
	public Zone(List<GraphEdge> _sides, int _zoneID) {
		super();
		//add the points of this shape
		ArrayList<Point> verticies = new ArrayList<Point>();
		for(GraphEdge curSide : _sides) {
			Point p1 = new Point((int)curSide.x1, (int)curSide.y1);
			Point p2 = new Point((int)curSide.x2, (int)curSide.y2);
			if(! verticies.contains(p1)) {
				verticies.add(p1);
			}
			if(!verticies.contains(p2)) {
				verticies.add(p2);
			}
		}
		
		//now that we have all the corners, add them to the shape
		for(Point p : verticies) {
			this.addPoint(p.x, p.y);
		}

		//store what this ID is
		zoneID = _zoneID;

		//now, store the neighbors
		neighbors = new int[_sides.size()];
		for(int i = 0; i < _sides.size(); i++) {
			GraphEdge curEdge = _sides.get(i);
			neighbors[i] = curEdge.site1 == zoneID ? curEdge.site2 : curEdge.site1;
		}
	}
	
	public Zone(Zone other) {
		super(other.xpoints, other.ypoints, other.npoints);
		this.zoneID = other.zoneID;
		this.neighbors = new int[other.neighbors.length];
		System.arraycopy(other.neighbors, 0, this.neighbors, 0, other.neighbors.length);
	}
	
	protected Zone(int _zoneID, Color _zoneColor) {
		super();
		zoneID = _zoneID;
	}
		
	public int getID() {
		return zoneID;
	}
	
	public double getCenterX() {
		return this.getBounds2D().getCenterX();
	}
	
	public double getCenterY() {
		return this.getBounds2D().getCenterY();
	}
	
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}
		
	public Circle2D getBroadcastArea(Point2D originator) {
		return new Circle2D(originator, getBroadcastRange());
	}
	
	public Circle2D getVisibilityArea(Point2D originator) {
		return new Circle2D(originator, getVisiblityRange());
	}
	
	public Circle2D getAudibleArea(Point2D originator) {
		return new Circle2D(originator, getAudibleRange());
	}
	
	public Circle2D getFoundArea(Point2D originator) {
		return new Circle2D(originator, getFoundRange());
	}

	
	public abstract Shout getShout(Survivor shouter);
	public abstract double getBroadcastRange();
	public abstract double getVisiblityRange();
	public abstract double getAudibleRange();
	public abstract double getFoundRange();
	
	public abstract boolean causesRepulsion();
	public abstract double repulsionMinDist();
	public abstract double repulsionMaxDist();
	public abstract double repulsionCurveShape();
	public abstract double repulsionScalingFactor();
	
	public abstract boolean isObstacle();

	public abstract Color getColor();
}
