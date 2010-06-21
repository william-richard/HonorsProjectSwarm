import java.awt.geom.Rectangle2D;
import java.util.List;


public class Victim extends Rectangle2D.Double {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENTION = 6; //Victims are squares really, so they only need 1 dimention.
	private final double shoutProb = .5; //The probability that victims will shout
	
	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/
	private double damage; //damage is a percentage of how hurt the victim is, with 1.0 being dead and 0.0 being totally healthy.
	private Zone currentZone; //TODO: Add this functionality, when I implement Zones
	
	public Victim(double centerX, double centerY, double _damage) {
		super();
		
		//determine where the top left corner of the Victim is - that's what Rectangle2D.Double wants
		double cornerX = centerX - DIMENTION/2;
		double cornerY = centerY - DIMENTION/2;
		//set the location of the rectangle
		setFrame(cornerX, cornerY, DIMENTION, DIMENTION);
		
		//store the damage of this Victim
		damage = _damage;
	}
	
	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public double getDamage() {
		return damage;
	}

	/***************************************************************************
	 * DOING METHODS
	 **************************************************************************/
	
	private Shout shout() {
		//see if we're going to shout
		if(World.RAMOM_GENERATOR.nextDouble() <= shoutProb) {
			//make and return a new shout
			//TODO: Make it so that Zone sets the shout radius
			//for now, use a constant
			double shoutRadius = DIMENTION*2;
			return new Shout(this.getCenterX(), this.getCenterY(), shoutRadius);
		}
		return null;
	}
	
	public void doStep(List<Shout> shouts) {
		//perhaps shout
		Shout newShout = shout();
		//see if we did shout
		if(newShout != null) {
			//we did shout - add it to the List
			shouts.add(newShout);
		}
	}
	
}
