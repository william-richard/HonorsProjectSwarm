import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;


public class Bot extends Rectangle implements Runnable {

	private static final long serialVersionUID = 1L;

	/***************************************************************************
	 * CONSTANTS
	 **************************************************************************/
	private final int DIMENSION = 6;
	private final double VISUAL_ID_VICTIM_PROB = .45;
	private final double HEAR_VICTIM_PROB = .75;
	private final double MOVE_RANDOMLY_PROB = .25;
	private final double BROADCAST_RADIUS = 50;
	private final double VISIBILITY_RADIUS = 12;
	private final double AUDITORY_RADIUS = 30;

	/***************************************************************************
	 * VARIABLES
	 **************************************************************************/

	/** These variables the bot does not know about - we store them for our convience. */
	private Zone currentZone; //what zone we actually are in can change some behavior
	private List<Shout> heardShouts; //the shouts that have been heard recently
	private Bot previousBot;
	private final Random numGen = new Random();

	private List<BotInfo> otherBotInfo; //storage of what information we know about all of the other Bots
	private String messageBuffer; //keep a buffer of messages from other robots
	private boolean keepGoing; //allows us to start or stop the robots
	private int botID;

	/***************************************************************************
	 * CONSTRUCTORS
	 **************************************************************************/
	public Bot(double centerX, double centerY, int _numBots, int _botID) {
		super();

		//first, in order to store our location, we need to find our top left corner
		double cornerX = centerX - DIMENSION/2;
		double cornerY = centerY - DIMENSION/2;

		//set our location and size
		setFrame(cornerX, cornerY, DIMENSION, DIMENSION);

		//now, set up the list of other bot information
		otherBotInfo = new ArrayList<BotInfo>();
		for(int i = 0; i < _numBots; i++) {
			BotInfo newBotInfo = new BotInfo(i);
			otherBotInfo.add(newBotInfo);
		}

		//set up other variables with default values
		messageBuffer = "";

		heardShouts = new ArrayList<Shout>();

		botID = _botID;
		
		currentZone = World.findZone(getCenterLocation());
	}

	/***************************************************************************
	 * GETTERS
	 **************************************************************************/
	public Point2D getCenterLocation() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}

	public Ellipse2D getBroadcastRadius() {
		//make a shape representing our broadcast range
		//should always be a circle, but we'll see

		//first, need to find the top left corner of the circle
		double broadcastRangeCornerX = this.getCenterX() - BROADCAST_RADIUS;
		double broadcastRangeCornerY = this.getCenterY() - BROADCAST_RADIUS;

		//now, make the broadcast range shape
		return new Ellipse2D.Double(broadcastRangeCornerX, broadcastRangeCornerY, BROADCAST_RADIUS*2, BROADCAST_RADIUS*2);
	}

	public Ellipse2D getVisibilityRadius() {
		//make a shape representing our view range
		//for now, it will be a circle, but that will change later

		//first, we need to figure out where the top left corner of the ellipse will be
		double viewRangeCornerX = this.getCenterX() - VISIBILITY_RADIUS;
		double viewRangeCornerY = this.getCenterY() - VISIBILITY_RADIUS;

		//now, make the visibility range
		return new Ellipse2D.Double(viewRangeCornerX, viewRangeCornerY, VISIBILITY_RADIUS*2, VISIBILITY_RADIUS*2);

	}

	public Ellipse2D getAuditbleRadius() {
		//make a shap representing our hearing range
		//for now, it will be a cirle, but that will change later

		//first, we need to figure out where the top left corner of the ellipse will be
		double audibleRangeCornerX = this.getCenterX() - AUDITORY_RADIUS;
		double audibleRangeCornerY = this.getCenterY() - AUDITORY_RADIUS;

		//now, make the auditory range
		return new Ellipse2D.Double(audibleRangeCornerX, audibleRangeCornerY, AUDITORY_RADIUS*2, AUDITORY_RADIUS*2);

	}

	public List<Shout> getShouts() {
		return heardShouts;
	}

	public int getID() {
		return botID;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Bot))
			return false;
		Bot other = (Bot) obj;
		if (botID != other.botID)
			return false;
		return true;
	}


	/***************************************************************************
	 * METHODS
	 **************************************************************************/
	private boolean recieveMessages = true;

	public void recieveMessage(String message) throws InterruptedException {
		//bad way to do this, but it'll be OK
		while(!recieveMessages) {
			wait(500);
		}
		messageBuffer = messageBuffer + message;
	}

	public void readMessages() {
		//make sure that we don't recieve any messages while we're coping the buffer
		recieveMessages = false;

		String newMessages = messageBuffer;
		messageBuffer = "";

		recieveMessages = true;

		//go through all the messages
		//messages should be split up by '\n'
		String[] messageArray = newMessages.split("\n");

		//make a scanner to make going through the messages a bit easier
		Scanner s;
		//go through the messages and update the stored info about the other bots
		for(String mes : messageArray) {
			print("Reading message '" + mes + "'");
			s = new Scanner(mes);

			if(! s.hasNextInt())
				continue;

			int botNum = s.nextInt();

			if(botNum == botID) {
				print("got message from myself - skip it");
				continue;
			}

			double newX = s.nextDouble();
			double newY = s.nextDouble();

			BotInfo newBotInfo = new BotInfo(botNum, newX, newY);

			otherBotInfo.get(botNum).merge(newBotInfo);
		}
	}

	private boolean listeningForShouts = true;

	public synchronized void hearShout(Shout s) throws InterruptedException {
		while(! listeningForShouts) {
			wait(500);
		}
		heardShouts.add(s);
	}


	private void move(Line2D.Double movementDirection) {
		//store our current state, so we can undo it if necessary.
		this.previousBot = (Bot) this.clone();

		//really, this method finds a point we want to move towards, and then calls "actuallyMoveTowards" or "actuallyMoveAway"

		//first, see if we have a direction to go in
		if(! movementDirection.getP1().equals(movementDirection.getP2())) {
			//assuming that the line goes from P1 -> P2
			//so basically, we want to head towards P2
			actuallyMoveTowards(movementDirection.getP2());
		}

		else {
			//find our nearest neighbor
			//or with some chance, move randomly
			//look in the array of BotInfo

			if(numGen.nextDouble() < MOVE_RANDOMLY_PROB) {
				moveRandomly();
			} else {
				int nearestBotIndex = 0;
				double nearestBotDistSq = java.lang.Double.MAX_VALUE;

				for(int i = 0; i < otherBotInfo.size(); i++) {
					BotInfo bi = otherBotInfo.get(i);

					//don't consider ourselves
					if(bi.getBotID() == botID) continue;

					double curDistSq = bi.getLocation().distanceSq(this.getCenterLocation());

					print("Got that " + bi.getBotID() + " is " + curDistSq + " away");

					if(curDistSq < nearestBotDistSq) {
						nearestBotDistSq = curDistSq;
						nearestBotIndex = i;
					}
				}

				print("Trying to move away from " + nearestBotIndex + " who is sqrt(" + nearestBotDistSq + ") away");

				//want to move away from the nearest bot
				actuallyMoveAway(otherBotInfo.get(nearestBotIndex).getLocation());
			}
		}

		//make sure we haven't moved off the screen
		if(! World.BOUNDING_BOX.contains(this)) {
			//we have - undo the move
			this.setLocation(previousBot.getLocation());
		}

		//we've now moved - broadcast location to nearby bots
		broadcastLocation();
	}

	private void moveRandomly() {
		int xChange, yChange;
		//50-50 chance to go left or right
		if(numGen.nextDouble() < .5) {
			xChange = DIMENSION;
		} else {
			xChange = -1 * DIMENSION;
		}

		//50-50 chance to go up or down
		if(numGen.nextDouble() < .5) {
			yChange = DIMENSION;
		} else {
			yChange = -1 * DIMENSION;
		}

		this.translate(xChange, yChange);
	}


	private void actuallyMoveTowards(Point2D p) {
		int xChange, yChange;

		//see if our x coordinates are greater than or less than than of p
		if(this.getCenterX() < p.getX()) {
			//in this case, we want to move in the positive X direction
			xChange = DIMENSION;
		} else {
			//we want to move in the negative x direction
			xChange = -1 * DIMENSION;
		}

		//do the same for the y coordinates
		if(this.getCenterY() < p.getY()) {
			//want to move in the positive Y direction
			yChange = DIMENSION;
		} else {
			//want to move in the negative Y direction
			yChange = -1 * DIMENSION;
		}	

		this.translate(xChange, yChange);
	}

	private void actuallyMoveAway(Point2D p) {
		int xChange, yChange;

		//see if our x coordinates are greater than or less than than of p
		if(this.getCenterX() < p.getX()) {
			//in this case, we want to move in the negative X direction
			xChange = -1 * DIMENSION;
		} else {
			//we want to move in the positive x direction
			xChange = DIMENSION;
		}

		//do the same for the y coordinates
		if(this.getCenterY() < p.getY()) {
			//want to move in the negative Y direction
			yChange = -1 * DIMENSION;
		} else {
			//want to move in the positive Y direction
			yChange = DIMENSION;
		}	

		this.translate(xChange, yChange);
	}

	@SuppressWarnings("unchecked")
	private void broadcastLocation() {
		//first, get our broadcast range
		Ellipse2D broadcastRange = getBroadcastRadius();

		//find any nearby bots
		List<Bot> nearbyBots = (List<Bot>) World.findIntersections(broadcastRange, World.allBots);

		//construct the message we want to send them.
		String outgoingMessage = botID + " " + this.getCenterX() + " " + this.getCenterY() + "\n";

		//send out the message to all the nearby bots
		for(Bot b : nearbyBots) {
			if(b.getID() == this.getID()) {
				continue;
			}
			try {
				b.recieveMessage(outgoingMessage);
			} catch (InterruptedException e) {
				//oh well - it didn't go through.  Don't worry about it, just go onto the next one
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<Victim> lookForVictims() {
		//first, get our visibility radius
		Ellipse2D visibilityRange = getVisibilityRadius();

		//see if the location of any of our victims intersects this range
		List<Victim> visibleVictims = (List<Victim>) World.findIntersections((Shape)visibilityRange, World.allVictims);

		print("In perfect world, would have just seen " + visibleVictims.size() + " victims");

		//visibileVictims is now a list of all the victims the robot could see if it was perfect
		//but it's not, and there is some probability that it will miss some of the victims
		//so, go through the list and remove some or all of the victims with that probability.
		//need to use an iterator, because it won't let us remove as we go otherwise
		ListIterator<Victim> vicIteratior = visibleVictims.listIterator();
		while(vicIteratior.hasNext()) {
			vicIteratior.next();
			if(! (numGen.nextDouble() <= VISUAL_ID_VICTIM_PROB)) {
				vicIteratior.remove(); //removes from the iterator AND the list
			}
		}

		print("Actually able to see " + visibleVictims.size() + " victims");

		//we have our list victims that the Bot saw - return it
		return visibleVictims;
	}

	private boolean goTowardsAVictim(List<Victim> visibleVicitms) {
		if(visibleVicitms.size() <= 0) {
			return false;
		}

		//want to go towards the nearest victim
		Victim nearestVic = null;
		double nearestDist = java.lang.Double.MAX_VALUE;


		//so, we need to figure out which one is the nearest one
		for(Victim v : visibleVicitms) {
			if(v.getCenterLocation().distanceSq(this.getCenterLocation()) < nearestDist) {
				nearestVic = v;
				nearestDist = v.getCenterLocation().distanceSq(this.getCenterLocation());
			}
		}

		//make a bee-line for that victim!
		move(new Line2D.Double(this.getCenterLocation(), nearestVic.getCenterLocation()));
		return true;
	}


	@SuppressWarnings("unchecked")
	private List<Shout> listenForVictims() {
		listeningForShouts = false;

		//first, get our auditory radius
		Ellipse2D auditoryRange = getAuditbleRadius();

		//see if any of the shouts we know about intersect this range
		List<Shout> audibleShouts = (List<Shout>) World.findIntersections((Shape) auditoryRange, heardShouts);

		print("In perfect world, would have just heard " + audibleShouts.size() + " vicitms");

		//audible shouts is now all the shouts we could hear if the robot could hear perfectly
		//but it can't - we're using a probability to model this fact
		//so, go through the list and remove the ones that probability says we can't identify
		ListIterator<Shout> shoutIterator = audibleShouts.listIterator();
		while(shoutIterator.hasNext()) {
			shoutIterator.next();
			if(! (numGen.nextDouble() <= HEAR_VICTIM_PROB)) {
				shoutIterator.remove();  //remove from iterator AND list
			}
		}

		listeningForShouts = true;

		print("Actually just heard " + audibleShouts.size() + " victims");

		//we have our list of shouts - return it
		return audibleShouts;	
	}

	private boolean goTowardsAShout(List<Shout> audibleShouts) {
		if(audibleShouts.size() <= 0) {
			return false;
		}

		//want to go towards the nearest victim
		Shout nearestShout = null;
		double nearestDist = java.lang.Double.MAX_VALUE;


		//so, we need to figure out which one is the nearest one
		for(Shout s : audibleShouts) {
			if(s.getCenterLocation().distanceSq(this.getCenterLocation()) < nearestDist) {
				nearestShout = s;
				nearestDist = s.getCenterLocation().distanceSq(this.getCenterLocation());
			}
		}

		//make a bee-line for that victim!
		move(new Line2D.Double(this.getCenterLocation(), nearestShout.getCenterLocation()));
		return true;
	}

	public void startBot() {
		keepGoing = true;
	}

	public void stopBot() {
		keepGoing = false;
	}


	public synchronized void run() {

		keepGoing = true;

		//first things first, make one move randomly, to spread out a bit
		moveRandomly();

		//first, see if we should keep going
		while(keepGoing) {
			boolean vicInSight = false;

			//first, read any messages that have come in, and take care of them
			readMessages();

			//now, try to move
			//we want to look around for victims, if we don't already know where one is
			if(!vicInSight) {
				List<Victim> visibleVics = lookForVictims();
				//if we find some, go towards one of them
				if(visibleVics.size() > 0) {
					vicInSight = true;
					goTowardsAVictim(visibleVics);
				}
			}

			//if we haven't already found a victim, try listening for one
			if(!vicInSight) {
				List<Shout> audibleShouts = listenForVictims();
				//if we can hear anything, go towards one of them
				if(audibleShouts.size() > 0) {
					vicInSight = true;
					goTowardsAShout(audibleShouts);
				}
			}

			//if we still haven't found a victim, just try to move
			if(!vicInSight) {
				move(new Line2D.Double()); //we don't have a direction to move in, so we're not passing a real line.
			}

			//now, just some housekeeping
			//we shouldn't hang onto shouts for too long
			heardShouts.clear();

			//make sure we are still in the zone we think we are in
			if(currentZone == null || ! currentZone.contains(getCenterLocation())) {
				currentZone = World.findZone(getCenterLocation());
			}
			
			try {
				this.wait(1000);
			} catch(InterruptedException e) {}
		}
	}

	private void print(String message) {
		boolean DEBUG = false;
		if(DEBUG) {
			System.out.println(botID + ":\t" + message);
		}
	}
}
