package simulation;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

import util.Utilities;


public class SurvivorPath extends Path2D.Double {
	
	private static final long serialVersionUID = 1L;
	
	private double pathLength;
	private double pathRating;
	private double avgRating;
	private Survivor sur;
	
	public SurvivorPath(Survivor _sur, double _pathLength, double _pathRating, double _avgRating, List<BotInfo> bots, Point2D endPoint) {
		super();
		sur = _sur;
		pathLength = _pathLength;
		pathRating = _pathRating;
		avgRating = _avgRating;

		//now, set up the path
		//start at the victim
		this.moveTo(sur.getCenterX(), sur.getCenterY());
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

	public Survivor getSur() {
		return sur;
	}
	
	@Override
	public String toString() {
		String retStr = "Vic loc: " + sur.getCenterX() + ", " + sur.getCenterY() + "\t" + "Length = "+ pathLength + "\tRating= " + pathRating +"\tAvgRating = " + avgRating + "\t Points: ";
		
		List<Point2D> verticies = Utilities.getVerticies(this);
		
		for(Point2D p : verticies) {
			retStr += "(" + p.getX() + ", " + p.getY() + ") ";
		}
		
		return retStr;
	}
	
	
	
	
	
	
	
}
