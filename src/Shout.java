import java.awt.geom.Ellipse2D;

public class Shout extends Ellipse2D.Double {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Shout(double centerX, double centerY, double radius) {
		super();
		
		//calculate the upper left corner x and y, which is what Ellipse2D wants
		double cornerX = centerX - radius/2;
		double cornerY = centerY - radius/2;
		
		//set the location of this Shout
		setFrame(cornerX, cornerY, radius, radius);
	}
	
	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public double getRadius() {
		return this.getHeight();
	}
}
