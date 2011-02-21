package zones;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import main.java.be.humphreys.voronoi.GraphEdge;

import simulation.Shout;
import simulation.Survivor;
import simulation.World;
import util.Utilities;
import util.shapes.Circle2D;

public abstract class Zone extends Polygon {

	private static final long serialVersionUID = -3828823916299213104L;

	protected int zoneID;
	protected int[] neighbors;

	private final int TOP_EDGE = 0;
	private final int BOTTOM_EDGE = 1;
	private final int LEFT_EDGE = 2;
	private final int RIGHT_EDGE = 3;

	public Zone(List<GraphEdge> _sides, int _zoneID, final Point2D center, BoundingBox bbox) {
		super();

		//store what this ID is
		zoneID = _zoneID;

		ArrayList<Point> verticies = new ArrayList<Point>();
		for(GraphEdge curSide : _sides) {

			Point p1 = new Point((int)curSide.x1, (int)curSide.y1);
			Point p2 = new Point((int)curSide.x2, (int)curSide.y2);
			
			if(p1.equals(p2)) {
				//weird artifact - handle it
				continue;
			}
			
			
			if(!verticies.contains(p1)) {
				verticies.add(p1);
			}
			if(!verticies.contains(p2)) {
				verticies.add(p2);
			}
		}
		
		//see if we need to add corners of the bounding box
		boolean[] requiredEdges = new boolean[4];
		Arrays.fill(requiredEdges, false);
		for(Point p : verticies) {
			if(World.BOUNDING_BOX.isPointOnBorder(p)) {
				if(p.getX() == bbox.getMinX()) {
					requiredEdges[LEFT_EDGE] = true;
				}
				if(p.getX() == bbox.getMaxX()) {
					requiredEdges[RIGHT_EDGE] = true;
				}
				if(p.getY() == bbox.getMinY()) {
					requiredEdges[TOP_EDGE] = true;
				}
				if(p.getY() == bbox.getMaxY()) {
					requiredEdges[BOTTOM_EDGE] = true;
				}
			}
		}

		//see which ones we need
		//and figure out which corner that corresponds to
		//catch if we only need to add 1 corner
		if(requiredEdges[LEFT_EDGE] && requiredEdges[TOP_EDGE]) {
			verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMinY()));
		}
		if(requiredEdges[RIGHT_EDGE] && requiredEdges[TOP_EDGE]) {
			verticies.add(new Point((int)bbox.getMaxX(), (int)bbox.getMinY()));
		}
		if(requiredEdges[LEFT_EDGE] && requiredEdges[BOTTOM_EDGE]) {
			verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMaxY()));
		}
		if(requiredEdges[RIGHT_EDGE] && requiredEdges[BOTTOM_EDGE]) {
			verticies.add(new Point((int) bbox.getMaxX(), (int)bbox.getMaxY()));
		}
		//catch if we need to add 2 corners
		if(requiredEdges[TOP_EDGE] && requiredEdges[BOTTOM_EDGE] && !requiredEdges[LEFT_EDGE] && !requiredEdges[RIGHT_EDGE]) {
			//we need to add 2 corners
			//get average know x value
			//and go to whichever side that is closest to
			double avgX = 0.0;
			for(Point p : verticies) {
				avgX += p.getX();
			}
			avgX = avgX / verticies.size();
			if(avgX - bbox.getMinX() < bbox.getMaxX() - avgX) {
				//closest to the left edge
				verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMinY()));
				verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMaxY()));
			} else {
				//closest to the right edge
				verticies.add(new Point((int)bbox.getMaxX(), (int)bbox.getMinY()));
				verticies.add(new Point((int)bbox.getMaxX(), (int)bbox.getMaxY()));
			}
		}
		if(requiredEdges[LEFT_EDGE] && requiredEdges[RIGHT_EDGE] && !requiredEdges[TOP_EDGE] && !requiredEdges[BOTTOM_EDGE]) {
			//we need to add 2 corners
			//get the avg Y value of known points
			//and go to whichever side the avg is closest too
			double avgY = 0.0;
			for(Point p : verticies) {
				avgY += p.getY();
			}
			avgY = avgY / verticies.size();
			if(avgY - bbox.getMinY() < bbox.getMaxY() - avgY) {
				//closest to the top edge
				verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMinY()));
				verticies.add(new Point((int)bbox.getMaxX(), (int)bbox.getMinY()));
			} else {
				//closest to the bottom edge
				verticies.add(new Point((int)bbox.getMaxX(), (int)bbox.getMaxY()));
				verticies.add(new Point((int)bbox.getMinX(), (int)bbox.getMaxY()));
			}
		}

		//sort the verticies clockwise around the center location
		Collections.sort(verticies, new Comparator<Point>() {

			@Override
			public int compare(Point o1, Point o2) {
				double angle1 = Utilities.getAngleFromZero(center, o1);
				double angle2 = Utilities.getAngleFromZero(center, o2);

				if(o1.getY() == center.getY() && o2.getY() == center.getY()) {
					//they are both on the same plane as center
					//see if they are on opposite sides
					if(o1.getX() > center.getX() && o2.getX() < center.getX()) {
						//o1 < o2
						return -1;
					} else if(o1.getX() < center.getX() && o2.getX() > center.getX()) {
						//o1 > o2
						return 1;
					} else {
						//o1 == o2
						return 0;
					}
				}
				if(o1.getY() == center.getY()) {
					if(o1.getX() > center.getX()) {
						//o1 < EVERYTHING
						return -1;
					} else {
						if(o2.getY() < center.getY()) {
							//o2 < o1
							return 1;
						} else {
							//o1 < o2
							return -1;
						}
					}
				}
				
				if(o2.getY() == center.getY()) {
					if(o2.getX() > center.getX()) {
						//o2 < EVERYTHING
						return 1;
					} else {
						if(o1.getY() < center.getY()) {
							//o1 < o2
							return -1;
						} else {
							//o2 < o1
							return 1;
						}
					}
				}
				
				
				if(o1.getY() < center.getY() && o2.getY() > center.getY()) {
					//points above the center go first
					//so o1 < o2
					return -1;
				}
				if(o1.getY() > center.getY() && o2.getY() < center.getY()) {
					//o2 < o1
					return 1;
				}
				
				//they are both above or below
				if(o1.getY() < center.getY()) {
					//they are above
					//angles closer to 0 go first
					if(angle1 < angle2) {return -1;} 
					else if(angle1 > angle2) {return 1;} 
					else {return 0;}
				} else { 
					//they are below
					//angles closer to pi go first
					if(angle1 > angle2) { return -1;}
					else if(angle1 < angle2) {return 1;}
					else {return 0;}
				}
			}
		});

		//now that we have all the corners of the shape in order, add them to the shape
		for(Point p : verticies) {
			this.addPoint(p.x, p.y);
		}
		
		//now, store the neighbors
		neighbors = new int[_sides.size()];
		for(int i = 0; i < _sides.size(); i++) {
			GraphEdge curEdge = _sides.get(i);
			neighbors[i] = (curEdge.site1 == zoneID ? curEdge.site2 : curEdge.site1);
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

	public List<Zone> getNeighbors() {
		ArrayList<Zone> neighborZones = new ArrayList<Zone>();
		for(int neighborID : neighbors) {
			neighborZones.add(World.allZones.get(new Integer(neighborID)));
		}
		return neighborZones;
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
