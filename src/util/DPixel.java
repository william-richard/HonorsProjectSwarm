package util;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

public class DPixel implements Comparable<DPixel> {
	private Point loc;
	private Set<Integer> parentZoneIds;
	//previous pixel in best path
	private Point2D previous;
	@Deprecated
	private double distanceToSource;
	
	public DPixel(Point p) {
		loc = p;
		previous = null;
		parentZoneIds = new HashSet<Integer>();
	}
	
	public DPixel(int _x, int _y) {
		this(new Point(_x, _y));
	}
	
	public DPixel(DPixel other) {
		loc = other.loc;
		parentZoneIds = new HashSet<Integer>(other.parentZoneIds);
		previous = other.previous;
	}
	
	@Deprecated
	public double getWeight() {
//		return getParentZone().getPathWeightPerPixel();
		return 0.0;
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
	public int getX() {
		return loc.x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return loc.y;
	}

	public Point getLocation() {
		return loc;
	}
	
	/**
	 * @return the parentZone
	 */
	public Set<Integer> getParentZoneIds() {
		return parentZoneIds;
	}
	
	protected void addParentZone(Integer newParentId) {
		parentZoneIds.add(newParentId);
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
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
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
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + loc.x + ", " + loc.y + ")";
	}

	@Override
	public int compareTo(DPixel o) {
		return Utilities.shouldEqualsZero(this.getDistanceToSource() - o.getDistanceToSource()) ? 0 : (this.getDistanceToSource() < o.getDistanceToSource() ? -1 : 1);
	}	
}

