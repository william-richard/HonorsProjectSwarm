package util;

import java.awt.Point;

import simulation.World;
import zones.Zone;

public class DPixel implements Comparable<DPixel> {
	private int x, y;
	private Zone parentZone;
	//previous pixel in best path
	private Point previous;
	@Deprecated
	private double distanceToSource;
	
	public DPixel(int _x, int _y) {
		x = _x;
		y = _y;
		//figure out what zone we're in
		parentZone = World.findZone(new Point(x,y));
		previous = null;
	}
	
	public DPixel(DPixel other) {
		x = other.x;
		y = other.y;
		parentZone = other.parentZone;
		previous = other.previous;
	}

	public double getWeight() {
		return getParentZone().getPathWeightPerPixel();
	}

	public Point getPrevious() {
		return previous;
	}

	public void setPrevious(Point _prev) {
		previous = _prev;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}

	public Point getLocation() {
		return new Point(x,y);
	}
	
	/**
	 * @return the parentZone
	 */
	public Zone getParentZone() {
		return parentZone;
	}

	/**
	 * @return the distanceToSource
	 */
	@Deprecated
	public double getDistanceToSource() {
		return distanceToSource;
	}

	/**
	 * @param distanceToSource the distanceToSource to set
	 */
	@Deprecated
	public void setDistanceToSource(double distanceToSource) {
		this.distanceToSource = distanceToSource;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DPixel))
			return false;
		DPixel other = (DPixel) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public int compareTo(DPixel o) {
		return Utilities.shouldEqualsZero(this.getDistanceToSource() - o.getDistanceToSource()) ? 0 : (this.getDistanceToSource() < o.getDistanceToSource() ? -1 : 1);
	}	
}

