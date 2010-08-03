package simulation;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Shout extends Area {
	
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_SHOUT_RADIUS = 20;
	public static final int DEFAULT_SHOUT_RADIUS_THROUGH_WALL = 10;
	private Victim shouter;
	

	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
//	public Shout(double centerX, double centerY, double radius) {
//		super();
//		
//		//calculate the upper left corner x and y, which is what Ellipse2D wants
//		double cornerX = centerX - radius/2;
//		double cornerY = centerY - radius/2;
//		
//		//set the location of this Shout
//		setFrame(cornerX, cornerY, radius, radius);
//	}

	public Shout(Shape s, Victim _shouter) {
		super(s);
		shouter = _shouter;
	}
	
	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
//	public double getRadius() {
//		return this.getHeight();
//	}
	
	public Point2D getCenterLocation() {
		//use the bonding box's center
		Rectangle2D bounds = this.getBounds2D();
		return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
	}
	
	public Victim getShouter() {
		return shouter;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Shout))
			return false;
		Shout other = (Shout) obj;
		Point2D thisCenterLocation = this.getCenterLocation();
		Point2D otherCenterLocation = other.getCenterLocation();
		
		if(thisCenterLocation == null) {
			if(otherCenterLocation != null) {
				return false;
			}
		} else if(! thisCenterLocation.equals(otherCenterLocation)) {
			return false;
		}
		return true;
	}
	
	
}
