package simulation;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import zones.Zone;


public class Victim extends Rectangle2D.Double {

	private static final long serialVersionUID = 1L;
	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6; //Victims are squares really, so they only need 1 dimention.
	private final double SHOUT_PROB = .75; //The probability that victims will shout

	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/
	private double damage; //damage is a percentage of how hurt the victim is, with 1.0 being dead and 0.0 being totally healthy.
	private Zone currentZone;


	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Victim(double centerX, double centerY, double _damage) {
		super();

		//determine where the top left corner of the Victim is - that's what Rectangle2D.Double wants
		double cornerX = centerX - DIMENSION/2;
		double cornerY = centerY - DIMENSION/2;
		//set the location of the rectangle
		setFrame(cornerX, cornerY, DIMENSION, DIMENSION);

		//store the damage of this Victim
		damage = _damage;

		//store the zones that we're in
		currentZone = World.findZone(getCenterLocation());
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public double getDamage() {
		return damage;
	}
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}


	/***************************************************************************
	 * METHODS
	 **************************************************************************/

	private synchronized void shout() {
		//see if we're going to shout
		if(World.RAMOM_GENERATOR.nextDouble() <= SHOUT_PROB) {
			//make and return a new shout
			Shout ourShout = currentZone.getShout(this);

			//send it off to all the bots - they'll determine if they can hear it or not
			for(Bot b : World.allBots) {
				try {
					b.hearShout(ourShout);
				} catch (InterruptedException e) {
					//it didn't make it for some reason
					//don't worry about it that much
				}
			}

		}

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Victim))
			return false;
		Victim other = (Victim) obj;
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

	@Override
	public int hashCode() {
		return this.getCenterLocation().hashCode();
	}

	public void doOneTimestep() {
		//each time, we want to try to shout
		shout();

		//don't need to update what zones we're in because we can't move
		//this fact may change later
	}
}
