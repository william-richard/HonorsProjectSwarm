import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Shout extends Area {
	
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_SHOUT_RADIUS = 20;
	

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

	public Shout(Shape s) {
		super(s);
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

}
