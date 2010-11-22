package simulation;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;


public class SurvivorPath extends Path2D.Double {

	private static final long serialVersionUID = 269945074800423928L;

	private double pathLength;
	private Survivor sur;

	public SurvivorPath(Survivor _sur, List<Point2D> pathPoints, Point2D endPoint) {
		super();
		sur = _sur;
		//now, construct the path
		//path will end up looking the same if we start at the end or at the start
		//for simplicity, start at the end
		this.moveTo(endPoint.getX(), endPoint.getY());
		//now, go through the rest of the points in reverse
		//should always have at least 1 point in the rest of the path
		//i.e. we should always have at least a start point and an end point

		for(int i = pathPoints.size() - 1; i >= 0; i--) {
			this.lineTo(pathPoints.get(i).getX(), pathPoints.get(i).getY());
		}
		//calculate the path length
		pathLength = 0.0;
		if(pathPoints.size() == 0) {
			//SHOULD never happen
			pathLength = 0;
		} else if(pathPoints.size() == 1) {
			pathLength = pathPoints.get(0).distance(endPoint);
		} else {
			for(int i = 1 ; i < pathPoints.size(); i++) {
				pathLength += pathPoints.get(i).distance(pathPoints.get(i-1));
			}
			pathLength += pathPoints.get(pathPoints.size()-1).distance(endPoint);
		}
	}

	public SurvivorPath(Survivor _sur, List<Point2D> pathPoints) {
		this(_sur, pathPoints.subList(0, pathPoints.size() - 1), pathPoints.get(pathPoints.size() - 1));
	}

	public double getPathLength() {
		return pathLength;
	}

	public Survivor getSur() {
		return sur;
	}




	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sur == null) ? 0 : sur.hashCode());
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
		if (!(obj instanceof SurvivorPath))
			return false;
		SurvivorPath other = (SurvivorPath) obj;
		if (sur == null) {
			if (other.sur != null)
				return false;
		} else if (!sur.equals(other.sur))
			return false;
		return true;
	}
}
