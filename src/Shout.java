import java.awt.geom.Ellipse2D;

public class Shout extends Ellipse2D.Double {
	
	public Shout(double centerX, double centerY, double width, double height) {
		super();
		
		//calculate the upper left corner x and y, which is what Ellipse2D wants
		double cornerX = centerX - width/2;
		double cornerY = centerY - height/2;
		
		//set the location of this Shout
		setFrame(cornerX, cornerY, width, height);
	}
	
	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public double getCeterX() {
		return this.getX() + this.getWidth()/2;
	}
	public double getCenterY() {
		return this.getY() + this.getHeight()/2;
	}
}
