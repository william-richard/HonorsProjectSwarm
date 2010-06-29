import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.List;


public class VictimPath extends Path2D.Double {
	
	private static final long serialVersionUID = 1L;
	
	private double pathLength;
	private double pathRating;
	private double avgRating;
	private Victim vic;
	
	public VictimPath(Victim _vic, double _pathLength, double _pathRating, double _avgRating, List<BotInfo> bots, Point2D endPoint) {
		super();
		vic = _vic;
		pathLength = _pathLength;
		pathRating = _pathRating;
		avgRating = _avgRating;

		//now, set up the path
		//start at the victim
		this.moveTo(vic.getCenterX(), vic.getCenterY());
		//now add all the bots
		for(BotInfo bi: bots) {
			this.lineTo(bi.getCenterX(), bi.getCenterY());
		}
		
		//finally, add the end point
		this.lineTo(endPoint.getX(), endPoint.getY());	
	}

	public double getPathLength() {
		return pathLength;
	}

	public double getPathRating() {
		return pathRating;
	}

	public double getAvgRating() {
		return avgRating;
	}

	public Victim getVic() {
		return vic;
	}
	
	@Override
	public String toString() {
		String retStr = "Vic loc: " + vic.getCenterX() + ", " + vic.getCenterY() + "\t" + "Length = "+ pathLength + "\tRating= " + pathRating +"\tAvgRating = " + avgRating + "\t Points: ";
		
		PathIterator pi = this.getPathIterator(null);
	
		double[] cur = new double[6];
		
		while(!pi.isDone()) {
			pi.currentSegment(cur);
			retStr += "(" + cur[0] + ", " + cur[1] + ") ";
			pi.next();
		}
		
		return retStr;
	}
	
	
	
	
	
	
	
}
