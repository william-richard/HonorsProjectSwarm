package util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import simulation.World;
import zones.Zone;

public class DPixel implements Comparable<DPixel> {
	private double x, y;
	private Zone parentZone;
	//previous pixel in best path
	private Point2D previous;
	@Deprecated
	private double distanceToSource;
	
	public DPixel(double _x, double _y) {
		x = _x;
		y = _y;
		//figure out what zone we're in
		parentZone = World.findZone(new Point2D.Double(x,y));
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

	public Point2D getPrevious() {
		return previous;
	}

	public void setPrevious(Point2D _prev) {
		previous = _prev;
	}

	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}

	public Point2D getLocation() {
		return new Point2D.Double(x,y);
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
		long temp;
		temp = java.lang.Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = java.lang.Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (java.lang.Double.doubleToLongBits(x) != java.lang.Double
				.doubleToLongBits(other.x))
			return false;
		if (java.lang.Double.doubleToLongBits(y) != java.lang.Double
				.doubleToLongBits(other.y))
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

