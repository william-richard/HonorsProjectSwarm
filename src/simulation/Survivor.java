package simulation;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Iterator;

import javax.swing.filechooser.FileNameExtensionFilter;

import util.Utilities;
import zones.Zone;


public class Survivor extends Rectangle2D.Double implements Serializable {

	private static final long serialVersionUID = -7790215006799035571L;
	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6; //survivors are squares really, so they only need 1 dimension.
	private final double SHOUT_PROB = .75; //The probability that survivors will shout
	private final Point2D CENTER_LOCATION;

	public static final FileNameExtensionFilter survivorFileExtensionFilter = new FileNameExtensionFilter("Serialized survivor file", "sur");

	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/
	private double damage; //damage is a percentage of how hurt the survivor is, with 1.0 being dead and 0.0 being totally healthy.
	transient private Zone currentZone;


	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Survivor(double centerX, double centerY, double _damage) {
		super();

		//determine where the top left corner of the survivor is - that's what Rectangle2D.Double wants
		double cornerX = centerX - DIMENSION/2;
		double cornerY = centerY - DIMENSION/2;
		//set the location of the rectangle
		setFrame(cornerX, cornerY, DIMENSION, DIMENSION);

		//store the damage of this survivor
		damage = _damage;

		//recreating the center location is taking up a lot of space
		//calculate once and store
		CENTER_LOCATION = new Point2D.Double(this.getCenterX(), this.getCenterY());

		//store the zones that we're in
		//do a bit of a cheat - if this survivor has been created, ask it which zone it is in
		if(World.allSurvivors.contains(this)) {
			currentZone = World.allSurvivors.get(World.allSurvivors.indexOf(this)).currentZone;
		} else {
			currentZone = World.findZone(getCenterLocation());
		}
	}
	
	public Survivor(Point2D location, double _damage) {
		this(location.getX(), location.getY(), _damage);
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public double getDamage() {
		return damage;
	}
	public Point2D getCenterLocation() {
		return CENTER_LOCATION;
	}


	/***************************************************************************
	 * METHODS
	 **************************************************************************/

	private void shout() {
		if(currentZone != null) {
			//see if we're going to shout
			if(World.RANDOM_GENERATOR.nextDouble() <= SHOUT_PROB) {
				//make and return a new shout
				Shout ourShout = currentZone.getShout(this);

				//add it to the global list
				World.allShouts.add(ourShout);

				//send it off to all the bots - they'll determine if they can hear it or not
				synchronized (World.allBots) {
					Bot b;
					Iterator<Bot> i = World.allBots.iterator();
					while(i.hasNext()) {
						b = i.next();
						b.hearShout(ourShout);
					}
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
		if (!(obj instanceof Survivor))
			return false;
		Survivor other = (Survivor) obj;
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

	public boolean deepEquals(Survivor other) {
		if(! this.equals(other)) 
			return false;
		if(! Utilities.shouldBeEqual(this.damage, other.damage)) 
			return false;
		return true;
	}



	public void doOneTimestep() {
		//each time, we want to try to shout
		shout();

		//don't need to update what zones we're in because we can't move
		//this fact may change later
	}

	private void readObject(ObjectInputStream in) throws IOException {
		try {
			in.defaultReadObject();
		} catch (ClassNotFoundException e) {
			System.out.println("Cannot find class while reading Survivor Object");
			e.printStackTrace();
		}

		//set up the zone correctly
		World.findZone(this.getCenterLocation());
	}



}
