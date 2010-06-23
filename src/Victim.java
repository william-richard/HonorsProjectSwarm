import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class Victim extends Rectangle2D.Double implements Runnable {

	//TODO Vicitms need to be threaded, and their shouts need to be sent just to the Bots that are nearby
	
	private static final long serialVersionUID = 1L;
	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6; //Victims are squares really, so they only need 1 dimention.
	private final int SHOUT_DISTANCE = 20;
	private final double SHOUT_PROB = .75; //The probability that victims will shout
	
	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/
	private double damage; //damage is a percentage of how hurt the victim is, with 1.0 being dead and 0.0 being totally healthy.
	private Zone currentZone; //TODO: Add this functionality, when I implement Zones
	
	private boolean keepGoing;
	
	
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
		
		//store the zone that we're in
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
			//TODO: Make it so that Zone sets the shout radius
			//for now, use a constant
			Shout ourShout = new Shout(this.getCenterX(), this.getCenterY(), SHOUT_DISTANCE);
			
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
	
	public synchronized void run() {
		keepGoing = true;
		
		while(keepGoing) {
			//each time, we want to try to shout
			shout();
			
			//don't need to update what zone we're in becasue we can't move
			//this fact may change later
			
			//then wait a bit
			try {
				wait(1000);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}	
	}
	
	
}
